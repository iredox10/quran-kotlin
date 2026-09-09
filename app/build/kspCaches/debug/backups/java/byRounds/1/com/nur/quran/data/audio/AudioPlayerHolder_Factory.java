package com.nur.quran.data.audio;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AudioPlayerHolder_Factory implements Factory<AudioPlayerHolder> {
  private final Provider<Context> appContextProvider;

  public AudioPlayerHolder_Factory(Provider<Context> appContextProvider) {
    this.appContextProvider = appContextProvider;
  }

  @Override
  public AudioPlayerHolder get() {
    return newInstance(appContextProvider.get());
  }

  public static AudioPlayerHolder_Factory create(Provider<Context> appContextProvider) {
    return new AudioPlayerHolder_Factory(appContextProvider);
  }

  public static AudioPlayerHolder newInstance(Context appContext) {
    return new AudioPlayerHolder(appContext);
  }
}
