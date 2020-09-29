package javax.microedition.lcdui;

public class ChoiceGroup extends Item implements Choice {
	public ChoiceGroup(String label, int choiceType) {
        super(label);
    }

    public int size() {
        return 0;
    }

    public String getString(int elementNum) {
        return null;
    }

    public Image getImage(int elementNum) {
        return null;
    }

    public int append(String stringPart, Image imagePart) {
        return 0;
    }

    public void insert(int elementNum, String stringPart, Image imagePart) {
    }

    public void delete(int elementNum) {
    }

    public void set(int elementNum, String stringPart, Image imagePart) {
    }

    public boolean isSelected(int elementNum) {
        return false;
    }

    public int getSelectedIndex() {
        return 0;
    }

    public int getSelectedFlags(boolean[] selectedArray_return) {
        return 0;
    }

    public void setSelectedIndex(int elementNum, boolean selected) {
    }

    public void setSelectedFlags(boolean[] selectedArray) {
    }
}
