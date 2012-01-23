package org.xwiki.gwt.wysiwyg.client.plugin.rt;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import fr.loria.score.client.ClientCallback;
import fr.loria.score.client.ClientDTO;
import fr.loria.score.client.Converter;
import fr.loria.score.jupiter.model.Document;
import fr.loria.score.jupiter.model.Message;
import fr.loria.score.jupiter.tree.TreeDocument;
import fr.loria.score.jupiter.tree.operation.*;

/**
 * Callback for tree documents, wysiwyg editor
 */
public class TreeClientCallback implements ClientCallback {
    private Node nativeNode;

    public TreeClientCallback(Node nativeNode) {
        this.nativeNode = nativeNode;
    }

    @Override
    public void onConnected(ClientDTO dto, Document document, boolean updateUI) {
        if (updateUI) {
            log.finest("Updating UI for WYSIWYG. Replacing native node: " + Element.as(nativeNode).getString());
            Node newNode = Converter.fromCustomToNative(((TreeDocument) document).getRoot());
            nativeNode.getParentNode().replaceChild(newNode, nativeNode);
            nativeNode = newNode;
            log.finest("New native node: " + Element.as(nativeNode).getString());
        }
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onExecute(Message receivedMessage) {
        log.fine("Executing received: " + receivedMessage);
        log.fine("Native node is before: " + Element.as(nativeNode).getString());
        TreeOperation operation = (TreeOperation) receivedMessage.getOperation();

        final int position = operation.getPosition();
        final int[] path = operation.getPath();
        final Node targetNode = TreeHelper.getChildNodeFromLocator(nativeNode, path);

        if (operation instanceof TreeInsertText) {
            //operates on a text node
            TreeInsertText insertText = (TreeInsertText) operation;
            String txt = String.valueOf(insertText.getText());
            Node newTextNode = com.google.gwt.dom.client.Document.get().createTextNode(txt);

            //some browsers insert a br element on an empty text area, so remove it
            Node brElement = targetNode.getChild(0);
            if (brElement != null && brElement.getNodeName().equalsIgnoreCase("br")) {
                targetNode.replaceChild(newTextNode, brElement);
            } else if (path.length == 1 && path[0] == 0) {
                targetNode.appendChild(newTextNode);
            } else {
                Text.as(targetNode).insertData(position, txt);
            }
        } else if (operation instanceof TreeDeleteText) {
            TreeDeleteText deleteText = (TreeDeleteText) operation;

            Text textNode = (Text) targetNode;
            textNode.deleteData(position, 1); //delete 1 char
        } else if (operation instanceof TreeInsertParagraph) {
            TreeInsertParagraph treeInsertParagraph = (TreeInsertParagraph) operation;
            log.info("1");
            // cases
            //1 hit enter on empty text area
            if (nativeNode.getChildCount() == 0) { //nativeNode == targetNode
                log.info("2");
                targetNode.insertFirst(com.google.gwt.dom.client.Document.get().createElement("p"));
            } else if (nativeNode.getChildCount() == 1 && nativeNode.getFirstChild().getNodeName().equalsIgnoreCase("br")) {
                log.info("3");
                nativeNode.replaceChild(nativeNode.getFirstChild(), com.google.gwt.dom.client.Document.get().createElement("p"));
                //2 hit enter on first line
            } else if (path.length == 1 && path[0] == 0) {
                log.info("3");
                //2.1 enter at the start of the text
                if (position == 0) { // pull down all lines
                    log.info("4");
                    targetNode.getParentNode().insertFirst(com.google.gwt.dom.client.Document.get().createElement("p"));
                } else {
                    log.info("5");
                    //split the line, assume the targetNode is text
                    String actualText = targetNode.getNodeValue();
                    //2.2 enter in the middle of the text
                    if (true) {//position < actualText.length()
                        log.info("6");
                        Text textNode = Text.as(targetNode);
                        textNode.deleteData(position, actualText.length() - position);

                        Node parentElement = targetNode.getParentElement();
                        Element p = DOM.createElement("p");
                        p.setInnerText(actualText.substring(position));
                        parentElement.insertAfter(textNode, p);
                    } else {
                    //2.2 enter at the end of the text

                    }
                }
                return;
            } else {
                log.info("7");
                handleNewParagraph(targetNode, position);
            }

            //3 hit enter in between
            //4 hit enter at the end

            //Get the actual text node
            //first remove from the textnode what was after caret position
        } else if (operation instanceof TreeMergeParagraph) {
            Node p = targetNode.getParentElement();
            Node pPreviousSibling = p.getPreviousSibling();

            targetNode.removeFromParent();
            p.removeFromParent();

            Node oldTextNode = pPreviousSibling.getChild(0);
            Text newTextNode = com.google.gwt.dom.client.Document.get().createTextNode(oldTextNode.getNodeValue() + targetNode.getNodeValue());
            pPreviousSibling.replaceChild(newTextNode, oldTextNode);
        }
        log.fine("Native node is after: " + Element.as(nativeNode).getString());
    }

    private void handleNewParagraph(Node targetNode, int position) {
            String actualText = targetNode.getNodeValue();
            targetNode.setNodeValue(actualText.substring(0, position));

            //then insert new node
            Node n = targetNode.getParentElement();
            Element p = DOM.createElement("p");
            p.setInnerText(actualText.substring(position));
            n.getParentElement().insertAfter(p, n);
    }
}