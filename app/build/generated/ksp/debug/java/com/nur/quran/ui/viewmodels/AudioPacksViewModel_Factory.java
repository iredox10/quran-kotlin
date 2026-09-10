package com.nur.quran.ui.viewmodels;

import android.content.Context;
import com.nur.quran.data.audio.AudioDownloadManager;
import com.nur.quran.data.repository.QuranRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AudioPacksViewModel_Factory implements Factory<AudioPacksViewModel> {
  private final Provider<AudioDownloadManager> audioDownloadManagerProvider;

  private final Provider<QuranRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public AudioPacksViewModel_Factory(Provider<AudioDownloadManager> audioDownloadManagerProvider,
      Provider<QuranRepository> repositoryProvider, Provider<Context> contextProvider) {
    this.audioDownloadManagerProvider = audioDownloadManagerProvider;
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AudioPacksViewModel get() {
    return newInstance(audioDownloadManagerProvider.get(), repositoryProvider.get(), contextProvider.get());
  }

  public static AudioPacksViewModel_Factory create(
      Provider<AudioDownloadManager> audioDownloadManagerProvider,
      Provider<QuranRepository> repositoryProvider, Provider<Context> contextProvider) {
    return new AudioPacksViewModel_Factory(audioDownloadManagerProvider, repositoryProvider, contextProvider);
  }

  public static AudioPacksViewModel newInstance(AudioDownloadManager audioDownloadManager,
      QuranRepository repository, Context context) {
    return new AudioPacksViewModel(audioDownloadManager, repository, context);
  }
}
