package vectorization;

public class ListWrapper {
	private String variableName;
    private String loopIterator;
    private Integer integerValue;
    private Integer loopIndexIterator;

    public ListWrapper(String s1, String s2, Integer i1, Integer i2){
    	setFirstStringValue(s1);
    	setSecondStringValue(s2);
    	setIntegerValue(i1);
    	setLoopIndexValue(i2);
    }
    
    
    public String getFirstStringValue() {
        return variableName;
    }

    public void setFirstStringValue(String firstStringValue) {
        this.variableName = firstStringValue;
    }

    public String getSecondStringValue() {
        return loopIterator;
    }

    public void setSecondStringValue(String secondStringValue) {
        this.loopIterator = secondStringValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }
    
    public Integer getLoopIndexValue() {
        return loopIndexIterator;
    }

    public void setLoopIndexValue(Integer integerValue) {
        this.loopIndexIterator = integerValue;
    }
}
