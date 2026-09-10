package com.nur.quran.data.words;

import android.content.Context;
import com.google.gson.Gson;
import com.nur.quran.data.api.QuranApi;
import com.nur.quran.data.db.dao.QuranDao;
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
public final class WordPackManager_Factory implements Factory<WordPackManager> {
  private final Provider<QuranApi> quranApiProvider;

  private final Provider<QuranDao> quranDaoProvider;

  private final Provider<Gson> gsonProvider;

  private final Provider<Context> contextProvider;

  public WordPackManager_Factory(Provider<QuranApi> quranApiProvider,
      Provider<QuranDao> quranDaoProvider, Provider<Gson> gsonProvider,
      Provider<Context> contextProvider) {
    this.quranApiProvider = quranApiProvider;
    this.quranDaoProvider = quranDaoProvider;
    this.gsonProvider = gsonProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public WordPackManager get() {
    return newInstance(quranApiProvider.get(), quranDaoProvider.get(), gsonProvider.get(), contextProvider.get());
  }

  public static WordPackManager_Factory create(Provider<QuranApi> quranApiProvider,
      Provider<QuranDao> quranDaoProvider, Provider<Gson> gsonProvider,
      Provider<Context> contextProvider) {
    return new WordPackManager_Factory(quranApiProvider, quranDaoProvider, gsonProvider, contextProvider);
  }

  public static WordPackManager newInstance(QuranApi quranApi, QuranDao quranDao, Gson gson,
      Context context) {
    return new WordPackManager(quranApi, quranDao, gson, context);
  }
}
