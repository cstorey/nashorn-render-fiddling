package widget;

import com.coveo.nashorn_modules.Module;
import com.coveo.nashorn_modules.Require;
import com.coveo.nashorn_modules.ResourceFolder;
import com.google.common.collect.Lists;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Test;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.google.common.math.Quantiles.percentiles;
import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.nanoTime;

public class WidgetTest {

    private static final String BUNDLE_JS = "./bundle.js";
    private static final String JS_DIST_PATH = "dist";
    private NashornScriptEngine engine = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
    private Module require = buildEngine();

    private Module buildEngine() {
        ResourceFolder rootFolder = ResourceFolder.create(getClass().getClassLoader(), JS_DIST_PATH, "UTF-8");
        try {
            return Require.enable(engine, rootFolder);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public class WidgetVM {
        private final String name;
        private final int num;
        private ScriptObjectMirror bundle = (ScriptObjectMirror) require.require(BUNDLE_JS);

        public WidgetVM(String name, int num) throws ScriptException {
            this.name = name;
            this.num = num;
        }

        private ScriptObjectMirror render() {
            err.println("Bundle: " + bundle);
            err.println("Bundle keys: " + Lists.newArrayList(bundle.getOwnKeys(false)));

            return (ScriptObjectMirror) bundle.callMember("component",
                    name, num);
        }
    }

    @Test
    public void testWidget() throws ScriptException {
        try {
            Module require = (Module) engine.eval("require");
            err.println("Require: " + require);

            WidgetVM model = new WidgetVM("frob", 42);
            ScriptObjectMirror component = model.render();
            Supplier<Object> thunk = () -> renderComponent(component);

            ArrayList<Double> samples = Lists.newArrayList();
            err.println(format("Thunk result: %s", thunk.get()));

            IntStream.range(0, 1024).forEach((_n) ->
                    timeItReal(thunk, samples::add));

            err.println(new TreeMap<>(percentiles().indexes(0, 50, 90, 95, 99, 100).compute(samples)));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private String renderComponent(ScriptObjectMirror component) {
        try {
            ScriptObjectMirror bundle = (ScriptObjectMirror) require.require(BUNDLE_JS);
            return (String) bundle.callMember("renderToStaticMarkup", component);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

    }

    private <T> T timeItReal(Supplier<T> callable, DoubleConsumer doubleConsumer) {
        long t0 = nanoTime();

        T res = callable.get();

        long t1 = nanoTime();

        double taken = (double) (t1 - t0) / 1000000.0;
        doubleConsumer.accept(taken);
        return res;
    }

}
