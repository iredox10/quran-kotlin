package com.nur.quran.data.tafsir;

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
public final class TafsirPackManager_Factory implements Factory<TafsirPackManager> {
  private final Provider<Context> contextProvider;

  private final Provider<QuranDao> quranDaoProvider;

  private final Provider<QuranApi> quranApiProvider;

  private final Provider<Gson> gsonProvider;

  public TafsirPackManager_Factory(Provider<Context> contextProvider,
      Provider<QuranDao> quranDaoProvider, Provider<QuranApi> quranApiProvider,
      Provider<Gson> gsonProvider) {
    this.contextProvider = contextProvider;
    this.quranDaoProvider = quranDaoProvider;
    this.quranApiProvider = quranApiProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public TafsirPackManager get() {
    return newInstance(contextProvider.get(), quranDaoProvider.get(), quranApiProvider.get(), gsonProvider.get());
  }

  public static TafsirPackManager_Factory create(Provider<Context> contextProvider,
      Provider<QuranDao> quranDaoProvider, Provider<QuranApi> quranApiProvider,
      Provider<Gson> gsonProvider) {
    return new TafsirPackManager_Factory(contextProvider, quranDaoProvider, quranApiProvider, gsonProvider);
  }

  public static TafsirPackManager newInstance(Context context, QuranDao quranDao, QuranApi quranApi,
      Gson gson) {
    return new TafsirPackManager(context, quranDao, quranApi, gson);
  }
}
