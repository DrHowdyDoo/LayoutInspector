package com.drhowdydoo.layoutinspector.service;

import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;
import android.util.Log;

public class AssistSessionService extends VoiceInteractionSessionService {
    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        Log.d("AssistantDebug", "AssistSessionService: onNewSession()");
        return (new AssistSession(this));
    }

}
