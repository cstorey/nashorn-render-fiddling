package widget;

import com.coveo.nashorn_modules.Module;
import com.coveo.nashorn_modules.Require;
import com.coveo.nashorn_modules.ResourceFolder;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.google.common.math.Quantiles.percentiles;
import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.nanoTime;

public class WidgetTest {

    private static final String DIST_BUNDLE_JS = "/dist/bundle.js";
    private ScriptEngine engine = buildEngine();

    private ScriptEngine buildEngine() {
        Supplier<NashornScriptEngine> callable = () -> (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
        NashornScriptEngine engine = callable.get();

        ResourceFolder rootFolder = ResourceFolder.create(getClass().getClassLoader(), "dist", "UTF-8");
        try {
            Require.enable(engine, rootFolder);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        return engine;
    }

    private DoubleConsumer showTime(String name) {
        return taken -> err.println(format("%s took %.6fms", name, taken));
    }

    @Test
    public void testWidget() throws ScriptException {

        try {
            Module require = (Module) eval(engine, "require");
            err.println("Require: " + require);

            ScriptObjectMirror bundle = (ScriptObjectMirror) require.require("./bundle.js");

            err.println("Bundle: " + bundle);

            ScriptObjectMirror component = (ScriptObjectMirror) bundle.callMember("component");
            Function<Object, String> renderToString = (Object it) -> (String) bundle.callMember("renderToStaticMarkup", it);

            ArrayList<Double> samples = Lists.newArrayList();
            Supplier<Object> thunk = () -> renderToString.apply(component);

            err.println(format("Thunk result: %s", thunk.get()));

            IntStream.range(0, 1024).forEach((_n) ->
                    timeItReal(thunk, samples::add));

            err.println(new TreeMap<>(percentiles().indexes(0, 50, 90, 95, 99, 100).compute(samples)));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStreamReader readResource(String name) {
        return new InputStreamReader(
                Verify.verifyNotNull(
                        getClass().getResourceAsStream(name)));
    }

    private <T> T timeItReal(Supplier<T> callable, DoubleConsumer doubleConsumer) {
        long t0 = nanoTime();

        T res = callable.get();

        long t1 = nanoTime();

        double taken = (double) (t1 - t0) / 1000000.0;
        doubleConsumer.accept(taken);
        return res;
    }

    private Object eval(ScriptEngine engine, InputStreamReader reader) {
        try {
            return engine.eval(reader);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private Object eval(ScriptEngine engine, String reader) {
        try {
            return engine.eval(reader);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
