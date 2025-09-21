package com.drhowdydoo.layoutinspector.model;

import android.app.assist.AssistStructure;
import android.view.View;

public class ViewNodeWrapper {

    private AssistStructure.ViewNode viewNode;
    private boolean visibility;
    private ArrowSet arrowSet;
    private int depth;


    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility) {
        this.viewNode = viewNode;
        this.visibility = visibility;
    }

    public ViewNodeWrapper(AssistStructure.ViewNode viewNode, boolean visibility, ArrowSet arrowSet) {
        this.viewNode = viewNode;
        this.visibility = visibility;
        this.arrowSet = arrowSet;
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

    public ArrowSet getArrowSet() {
        return arrowSet;
    }

    public void setArrowSet(ArrowSet arrowSet) {
        this.arrowSet = arrowSet;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
