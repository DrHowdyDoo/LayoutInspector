package com.drhowdydoo.layoutinspector.util;

import android.app.assist.AssistStructure;
import android.view.View;

public class ViewNodeWrapper {

    private AssistStructure.ViewNode viewNode;
    private boolean visibility;


    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility) {
        this.viewNode = viewNode;
        this.visibility = visibility;
    }

    public AssistStructure.ViewNode getViewNode() {
        return viewNode;
    }

    public void setViewNode(AssistStructure.ViewNode viewNode) {
        this.viewNode = viewNode;
    }

    public boolean isVisible() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public boolean showViewNode(int visibilityFlag){
        return (visibilityFlag == View.VISIBLE) == visibility;
    }
}
