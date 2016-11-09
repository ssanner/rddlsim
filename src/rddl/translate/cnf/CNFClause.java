package rddl.translate.cnf;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class CNFClause {
	public ArrayList<Literal> _literals = null;
	/* Clause Type is defined as below
	 * INIT:		starting state
	 * CONSTRAINT:	frame axioms
	 * EFFECT:		for clauses in form of "action and conditions implies new state"
	 * OBSERVATION:	for observation, not used
	 * COST:		for clauses in form of "action and conditions implies cost"
	 * */
	public enum ClauseSources { INIT,CONSTRAINT,EFFECT,OBSERVATION,COST};
	public final static Double DETERMINISTIC1 = 1.0;
	public final static Double DETERMINISTIC0 = 0.0;
	// Whether new variables are generated or not
	public boolean isGend = false;
	// Only one of _probability/_weight can be used at same time. Mutual exclusive
	private Double _probability = null;
	private Double _weight = null;	
	private ClauseSources _source = null;
	// Record whether the clause is valid or not
	private boolean _valid = false;
	// Flag of clause, shows is there a chance variable.
	private boolean _probabilistic = false;
	


	public CNFClause()
	{
		_literals = new ArrayList<Literal>();
		_source = ClauseSources.INIT;
		
	}
	
	public CNFClause(ClauseSources type)
	{
		_literals = new ArrayList<Literal>();
		_source = type;
		/*
		 * Only OBSERVATION/EFFECT/COST need new variable.
		 * */
		if(_source == ClauseSources.OBSERVATION || 
				_source == ClauseSources.EFFECT ||
				_source == ClauseSources.COST)
		{
			isGend = false;
		}
		else
		{
			isGend = true;
		}
	}
	
	public void addLiteral(Literal l)
	{
		if(l == null)
		{
			System.out.println("Error: Try to add Null to litera list.");
			return;
		}
		if(_literals.size() == 0) _literals.add(l);
		else
		{
			for(Literal literal : _literals)
			{
				if(literal.getSymbol() == l.getSymbol())
				{
					if(literal.getNegative() == l.getNegative() &&
							literal._currentTS == l._currentTS)
					{
						return;
					}else if(literal._currentTS == l._currentTS)
					{
						_valid = true;
					}
					_literals.add(l);
					return;
				}
			}
			_literals.add(l);
		}
		return;
	}

	public void setProbability(Double probability)
	{
		if(_source == ClauseSources.EFFECT ||
				_source == ClauseSources.OBSERVATION)
		{
			_probability = probability;
			if(_probability != null && _probability < DETERMINISTIC1 && _probability > DETERMINISTIC0)
			{
				_probabilistic = true;
			}
			else
			{
				_probabilistic = false;
			}
		}
		else
		{
			//For now, only support probability distribution over action effection/Observation
		}
	}
	
	public void setWeight(Double weight)
	{
		if(_source == ClauseSources.COST)
		{
			_weight = weight;
		}
		else
		{}
	}

	public boolean isProbabilistic()
	{
		return _probabilistic;
	}
	
	public ClauseSources getSource()
	{
		return _source;
	}
	
	public Double getProbability()
	{
		return _probability;
	}

	public void printCNFClause(PrintWriter pw , HashMap varMapping, Integer timestep, Integer varNumberPerTS)
	{
		for(Literal l : _literals)
		{
			if(l == null)continue;
			Integer index = (Integer)varMapping.get(l.getSymbol());
			Integer no = null;
			if(l._currentTS)
				no = index + timestep * varNumberPerTS;
			else no = index + (timestep + 1) * varNumberPerTS;
			pw.print(l.getSign()+ no+ " ");
		}
	}
	
	// Create a new instance with the same value of this.
	public CNFClause clone()
	{
		CNFClause copy = new CNFClause(_source);
		copy._literals.addAll(_literals);
		copy.setProbability(_probability);
		copy.setWeight(_weight);
		copy._valid = _valid;
		copy.isGend = isGend;
		return copy;
	}

	public Double getWeight()
	{
		return _weight;
	}
}
