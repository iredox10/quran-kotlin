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
public final class AudioDownloadManager_Factory implements Factory<AudioDownloadManager> {
  private final Provider<Context> contextProvider;

  public AudioDownloadManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AudioDownloadManager get() {
    return newInstance(contextProvider.get());
  }

  public static AudioDownloadManager_Factory create(Provider<Context> contextProvider) {
    return new AudioDownloadManager_Factory(contextProvider);
  }

  public static AudioDownloadManager newInstance(Context context) {
    return new AudioDownloadManager(context);
  }
}
