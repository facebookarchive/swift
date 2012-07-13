package com.facebook.nifty.guice;

import com.facebook.nifty.core.NettyConfigBuilder;
import com.facebook.nifty.core.ThriftServerDef;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import javax.inject.Provider;
import javax.inject.Singleton;

public abstract class NiftyModule extends AbstractModule {

  private boolean configBound = false;

  @Override
  protected void configure() {
    configureNifty();
  }

  @Provides
  @Singleton
  public ChannelGroup getChannelGroup() {
    return new DefaultChannelGroup();
  }

  public NiftyModule withDefaultNettyConfig() {
    if (!configBound) {
      binder().bind(NettyConfigBuilder.class).toInstance(new NettyConfigBuilder());
      configBound = true;
      return this;
    }
    throw iae();
  }

  public NiftyModule withNettyConfig(Class<? extends Provider<NettyConfigBuilder>> provider) {
    if (!configBound) {
      binder().bind(NettyConfigBuilder.class).toProvider(provider);
      configBound = true;
      return this;
    }
    throw iae();
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

  private IllegalStateException iae() {
    return new IllegalStateException(
      "config already bound ! call useDefaultNettyConfig or withNettyConfig only once."
    );
  }
}
