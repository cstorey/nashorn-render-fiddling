package widget;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStreamReader;
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

    private static final String REACT_JS = "/META-INF/resources/webjars/react/15.3.2/react.js";
    private static final String REACT_DOM_SERVER_JS = "/META-INF/resources/webjars/react/15.3.2/react-dom-server.js";
    private ScriptEngine engine = buildEngine();

    private ScriptEngine buildEngine() {
        ScriptEngine engine = timeIt(() -> new ScriptEngineManager().getEngineByName("nashorn"),
                showTime("new nashorn"));

        eval(engine, "global  = this;");

        timeIt(() -> eval(engine, readResource(REACT_JS)),
                showTime("react-js"));
        timeIt(() -> eval(engine, readResource(REACT_DOM_SERVER_JS)),
                showTime("react-dom-server-js"));
        return engine;
    }

    private DoubleConsumer showTime(String name) {
        return taken -> err.println(format("%s took %.6fms", name, taken));
    }


    @Test
    public void testWidget() throws ScriptException {
        ScriptObjectMirror renderToString = timeIt(
                () -> (ScriptObjectMirror) eval(engine, "ReactDOMServer.renderToStaticMarkup"),
                showTime("renderToString"));

        String code = "function() { return (React.DOM.span(null, 'Hello world')); }";
        ScriptObjectMirror component = timeIt(
                () -> (ScriptObjectMirror) eval(engine, code),
                showTime("component"));

        ArrayList<Double> samples = Lists.newArrayList();
        Supplier<Object> thunk = () -> renderToString.call(null, component.call(null));
        err.println(format("Thunk result: %s", thunk.get()));
        IntStream.range(0, 1024).forEach((_n) -> timeIt(thunk, samples::add));

        err.println(new TreeMap<>(percentiles().indexes(0, 50, 90, 95, 99, 100).compute(samples)));
    }


    private InputStreamReader readResource(String name) {
        return new InputStreamReader(
                Verify.verifyNotNull(
                getClass().getResourceAsStream(name)));
    }

    private <T> T timeIt(Supplier<T> callable, DoubleConsumer doubleConsumer) {
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
