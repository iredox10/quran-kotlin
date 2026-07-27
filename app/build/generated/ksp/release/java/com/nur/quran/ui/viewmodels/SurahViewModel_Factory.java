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
public final class SurahViewModel_Factory implements Factory<SurahViewModel> {
  private final Provider<QuranRepository> repositoryProvider;

  private final Provider<AudioDownloadManager> audioDownloadManagerProvider;

  private final Provider<Context> contextProvider;

  public SurahViewModel_Factory(Provider<QuranRepository> repositoryProvider,
      Provider<AudioDownloadManager> audioDownloadManagerProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.audioDownloadManagerProvider = audioDownloadManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SurahViewModel get() {
    return newInstance(repositoryProvider.get(), audioDownloadManagerProvider.get(), contextProvider.get());
  }

  public static SurahViewModel_Factory create(Provider<QuranRepository> repositoryProvider,
      Provider<AudioDownloadManager> audioDownloadManagerProvider,
      Provider<Context> contextProvider) {
    return new SurahViewModel_Factory(repositoryProvider, audioDownloadManagerProvider, contextProvider);
  }

  public static SurahViewModel newInstance(QuranRepository repository,
      AudioDownloadManager audioDownloadManager, Context context) {
    return new SurahViewModel(repository, audioDownloadManager, context);
  }
}
