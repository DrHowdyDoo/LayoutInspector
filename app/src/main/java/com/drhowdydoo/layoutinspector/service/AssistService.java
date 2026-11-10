package com.drhowdydoo.layoutinspector.service;

import android.service.voice.VoiceInteractionService;
import android.util.Log;

public class AssistService extends VoiceInteractionService {

    @Override
    public void onReady() {
        super.onReady();
        Log.d("AssistantDebug", "AssistService: onReady()");
    }
}
