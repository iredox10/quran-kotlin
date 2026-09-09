package com.nur.quran.services;

import com.nur.quran.data.audio.AudioPlayerHolder;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class QuranAudioService_MembersInjector implements MembersInjector<QuranAudioService> {
  private final Provider<AudioPlayerHolder> audioPlayerHolderProvider;

  public QuranAudioService_MembersInjector(Provider<AudioPlayerHolder> audioPlayerHolderProvider) {
    this.audioPlayerHolderProvider = audioPlayerHolderProvider;
  }

  public static MembersInjector<QuranAudioService> create(
      Provider<AudioPlayerHolder> audioPlayerHolderProvider) {
    return new QuranAudioService_MembersInjector(audioPlayerHolderProvider);
  }

  @Override
  public void injectMembers(QuranAudioService instance) {
    injectAudioPlayerHolder(instance, audioPlayerHolderProvider.get());
  }

  @InjectedFieldSignature("com.nur.quran.services.QuranAudioService.audioPlayerHolder")
  public static void injectAudioPlayerHolder(QuranAudioService instance,
      AudioPlayerHolder audioPlayerHolder) {
    instance.audioPlayerHolder = audioPlayerHolder;
  }
}
