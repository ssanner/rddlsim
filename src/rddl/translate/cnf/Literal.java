package rddl.translate.cnf;

public class Literal {
	public final static String NEGATIVE = "-".intern();
	public final static String POSITIVE = "".intern();
	public boolean _currentTS = true;
	

	private boolean _negative = false;
	private String _sign = null;
	private String _symbol = null;
	
	public Literal(String symbol, boolean inCurrentTimeStep)
	{
		_symbol = symbol;
		_sign = POSITIVE;
		_currentTS = inCurrentTimeStep;
	}
	
	public Literal(boolean negation, String symbol, boolean inCurrentTimeStep)
	{
		if(negation)
		{
			_negative = negation;
			_sign = NEGATIVE;
		}
		else
		{
			_sign = POSITIVE;
		}
		_symbol = symbol;
		_currentTS = inCurrentTimeStep;
	}
	
	public void setNegative(boolean v)
	{
		if(v)
		{
			_negative = v;
			_sign = NEGATIVE;
		}
		else
		{
			_negative = v;
			_sign = POSITIVE;
		}
	}
	
	public boolean getNegative()
	{
		return _negative;
	}
	
	public String toString()
	{
		return _sign+_symbol;
	}
	
	public String getSymbol()
	{
		return _symbol;
	}
	
	public String getSign()
	{
		return _sign;
	}

}
