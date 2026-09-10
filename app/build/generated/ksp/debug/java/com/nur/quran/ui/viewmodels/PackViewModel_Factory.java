package com.nur.quran.ui.viewmodels;

import com.nur.quran.data.tafsir.TafsirPackManager;
import com.nur.quran.data.words.WordPackManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class PackViewModel_Factory implements Factory<PackViewModel> {
  private final Provider<TafsirPackManager> tafsirPackManagerProvider;

  private final Provider<WordPackManager> wordPackManagerProvider;

  public PackViewModel_Factory(Provider<TafsirPackManager> tafsirPackManagerProvider,
      Provider<WordPackManager> wordPackManagerProvider) {
    this.tafsirPackManagerProvider = tafsirPackManagerProvider;
    this.wordPackManagerProvider = wordPackManagerProvider;
  }

  @Override
  public PackViewModel get() {
    return newInstance(tafsirPackManagerProvider.get(), wordPackManagerProvider.get());
  }

  public static PackViewModel_Factory create(Provider<TafsirPackManager> tafsirPackManagerProvider,
      Provider<WordPackManager> wordPackManagerProvider) {
    return new PackViewModel_Factory(tafsirPackManagerProvider, wordPackManagerProvider);
  }

  public static PackViewModel newInstance(TafsirPackManager tafsirPackManager,
      WordPackManager wordPackManager) {
    return new PackViewModel(tafsirPackManager, wordPackManager);
  }
}
