package dev.dking.dev_machine_tools.is_this_thing_on;

import java.net.http.HttpRequest;

import com.google.cloud.functions.CloudEventsFunction;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import io.cloudevents.CloudEvent;

public class CloudFunction implements CloudEventsFunction {

  static {
    Thread.currentThread().setContextClassLoader(CloudFunction.class.getClassLoader());
    IFn require = Clojure.var("clojure.core", "require");
    require.invoke(Clojure.read("dev.dking.dev-machine-tools.is-this-thing-on"));
  }

  private static final IFn accept_impl = Clojure.var("dev.dking.dev-machine-tools.is-this-thing-on", "accept");

  @Override
  public void accept(CloudEvent event) throws Exception {
    accept_impl.invoke(event);
  }
}