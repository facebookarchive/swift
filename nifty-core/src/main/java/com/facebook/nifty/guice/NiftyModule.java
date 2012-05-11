package com.facebook.nifty.guice;

import com.facebook.nifty.core.ThriftServerDef;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Provider;

/**
 * Author @jaxlaw
 * Date: 4/22/12
 * Time: 10:25 PM
 */
public abstract class NiftyModule extends AbstractModule {
  @Override
  protected void configure() {
    configureNifty();
  }

  /**
   * User of Nifty via guice should override this method and use the little DSL defined here.
   */
  protected abstract void configureNifty();

  protected NiftyBuilder bind() {
    return new NiftyBuilder();
  }

  protected class NiftyBuilder {

    public NiftyBuilder() {
    }

    public void toInstance(ThriftServerDef def) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().toInstance(def);
    }

    public void toProvider(Class<? extends Provider<ThriftServerDef>> provider) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().toProvider(provider).asEagerSingleton();
    }

    public void toProvider(com.google.inject.Provider<? extends ThriftServerDef> provider) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().toProvider(provider).asEagerSingleton();
    }

    public void toProvider(com.google.inject.TypeLiteral<? extends javax.inject.Provider<? extends ThriftServerDef>> typeLiteral) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().toProvider(typeLiteral).asEagerSingleton();
    }

    public void toProvider(com.google.inject.Key<? extends javax.inject.Provider<? extends ThriftServerDef>> key) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().toProvider(key).asEagerSingleton();
    }

    public void to(Class<? extends ThriftServerDef> clazz) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().to(clazz).asEagerSingleton();
    }

    public void to(com.google.inject.TypeLiteral<? extends ThriftServerDef> typeLiteral) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().to(typeLiteral).asEagerSingleton();
    }

    public void to(com.google.inject.Key<? extends ThriftServerDef> key) {
      Multibinder.newSetBinder(binder(), ThriftServerDef.class)
        .addBinding().to(key).asEagerSingleton();
    }
  }
}
