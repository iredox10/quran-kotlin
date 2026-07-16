package com.nur.quran.di;

import com.nur.quran.data.api.QuranApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideQuranApiFactory implements Factory<QuranApi> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public AppModule_ProvideQuranApiFactory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public QuranApi get() {
    return provideQuranApi(okHttpClientProvider.get());
  }

  public static AppModule_ProvideQuranApiFactory create(
      Provider<OkHttpClient> okHttpClientProvider) {
    return new AppModule_ProvideQuranApiFactory(okHttpClientProvider);
  }

  public static QuranApi provideQuranApi(OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideQuranApi(okHttpClient));
  }
}
