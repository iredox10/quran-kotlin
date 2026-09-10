package com.nur.quran.data.tafsir;

import android.content.Context;
import com.google.gson.Gson;
import com.nur.quran.data.api.QuranApi;
import com.nur.quran.data.db.dao.QuranDao;
import com.nur.quran.data.words.WordPackManager;
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
public final class TafsirPackManager_Factory implements Factory<TafsirPackManager> {
  private final Provider<Context> contextProvider;

  private final Provider<QuranDao> quranDaoProvider;

  private final Provider<QuranApi> quranApiProvider;

  private final Provider<Gson> gsonProvider;

  private final Provider<WordPackManager> wordPackManagerProvider;

  public TafsirPackManager_Factory(Provider<Context> contextProvider,
      Provider<QuranDao> quranDaoProvider, Provider<QuranApi> quranApiProvider,
      Provider<Gson> gsonProvider, Provider<WordPackManager> wordPackManagerProvider) {
    this.contextProvider = contextProvider;
    this.quranDaoProvider = quranDaoProvider;
    this.quranApiProvider = quranApiProvider;
    this.gsonProvider = gsonProvider;
    this.wordPackManagerProvider = wordPackManagerProvider;
  }

  @Override
  public TafsirPackManager get() {
    return newInstance(contextProvider.get(), quranDaoProvider.get(), quranApiProvider.get(), gsonProvider.get(), wordPackManagerProvider.get());
  }

  public static TafsirPackManager_Factory create(Provider<Context> contextProvider,
      Provider<QuranDao> quranDaoProvider, Provider<QuranApi> quranApiProvider,
      Provider<Gson> gsonProvider, Provider<WordPackManager> wordPackManagerProvider) {
    return new TafsirPackManager_Factory(contextProvider, quranDaoProvider, quranApiProvider, gsonProvider, wordPackManagerProvider);
  }

  public static TafsirPackManager newInstance(Context context, QuranDao quranDao, QuranApi quranApi,
      Gson gson, WordPackManager wordPackManager) {
    return new TafsirPackManager(context, quranDao, quranApi, gson, wordPackManager);
  }
}
