////////////////////////////////////////////////////////////////////////////////
//
//	RMG - Reaction Mechanism Generator
//
//	Copyright (c) 2002-2011 Prof. William H. Green (whgreen@mit.edu) and the
//	RMG Team (rmg_dev@mit.edu)
//
//	Permission is hereby granted, free of charge, to any person obtaining a
//	copy of this software and associated documentation files (the "Software"),
//	to deal in the Software without restriction, including without limitation
//	the rights to use, copy, modify, merge, publish, distribute, sublicense,
//	and/or sell copies of the Software, and to permit persons to whom the
//	Software is furnished to do so, subject to the following conditions:
//
//	The above copyright notice and this permission notice shall be included in
//	all copies or substantial portions of the Software.
//
//	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//	FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
//	DEALINGS IN THE SOFTWARE.
//
////////////////////////////////////////////////////////////////////////////////

package jing.rxn;

import jing.param.Pressure;
import jing.param.Temperature;
import jing.rxnSys.Logger;

/**
 * A model of pressure-dependent reaction kinetics k(T, P) of the form
 * 
 * k(T, P) = A(P) * T ^ n(P) * exp( -Ea(P) / R * T )
 *
 * The values of A(P), n(P), and Ea(P) are stored for multiple pressures and
 * interpolation on a log P scale is used.
 *
 * @author jwallen
 */
public class PDepArrheniusKinetics implements PDepKinetics {

	/**
	 * The list of pressures at which we have Arrhenius parameters.
	 */
	public Pressure[] pressures;

	/**
	 * The list of Arrhenius kinetics fitted at each pressure.
	 */
	private ArrheniusKinetics[] kinetics;
	
	protected int numPressures = 0;

	public PDepArrheniusKinetics(int numP) {
		pressures = new Pressure[numP];
		kinetics = new ArrheniusKinetics[numP];
		setNumPressures(numP);
	}
	
	public void setKinetics(int index, Pressure P, ArrheniusKinetics kin) {
		if (index < 0 || index >= pressures.length)
			return;
		pressures[index] = P;
		kinetics[index] = kin;
	}

	/**
	 * Calculate the rate cofficient at the specified conditions.
	 * @param T The temperature of interest
	 * @param P The pressure of interest
	 * @return The rate coefficient evaluated at T and P
	 */
	public double calculateRate(Temperature T, Pressure P) throws POutOfRangeException {
		int index1 = -1; int index2 = -1;

		for (int i = 0; i < pressures.length - 1; i++) {
			if (pressures[i].getBar() <= P.getBar() && P.getBar() <= pressures[i+1].getBar()) {
				index1 = i; index2 = i + 1;
			}
		}

		if (index1 < 0 || index2 < 0)
		{
			Logger.warning(String.format("Tried to evaluate rate coefficient at P=%s Atm, which is outside valid range:",P.getAtm()));
			Logger.warning(toChemkinString());
			throw new POutOfRangeException(); // maybe we should return the limiting value closest to the desired pressure.
		}

		double logk1 = Math.log10(kinetics[index1].calculateRate(T));
		double logk2 = Math.log10(kinetics[index2].calculateRate(T));
		double logP0 = Math.log10(P.getBar());
		double logP1 = Math.log10(pressures[index1].getBar());
		double logP2 = Math.log10(pressures[index2].getBar());

		double logk0 = logk1 + (logk2 - logk1) / (logP2 - logP1) * (logP0 - logP1);

		return Math.pow(10, logk0);
	}

    public String toChemkinString() {
        String result = "";
		for (int i = 0; i < pressures.length; i++) {
			double Ea_in_kcalmol = kinetics[i].getEValue();
			double Ea = 0.0;
			if (ArrheniusKinetics.getEaUnits().equals("kcal/mol"))		Ea = Ea_in_kcalmol;
			else if (ArrheniusKinetics.getEaUnits().equals("cal/mol"))	Ea = Ea_in_kcalmol * 1000.0;
			else if (ArrheniusKinetics.getEaUnits().equals("kJ/mol"))	Ea = Ea_in_kcalmol * 4.184;
			else if (ArrheniusKinetics.getEaUnits().equals("J/mol"))	Ea = Ea_in_kcalmol * 4184.0;
			else if (ArrheniusKinetics.getEaUnits().equals("Kelvins"))	Ea = Ea_in_kcalmol / 1.987e-3;
			result += String.format("PLOG / %10s    %10.2e  %10s  %10s /\n",
									pressures[i].getAtm(),
									kinetics[i].getAValue(),
									kinetics[i].getNValue(),
									Ea);
		}
		return result;
    }
    
    public void setNumPressures(int numP) {
    	if (numP > getNumPressures()) numPressures = numP;
    }
    
    public int getNumPressures() {
    	return numPressures;
    }
    
    public ArrheniusKinetics getKinetics(int i) {
    	return kinetics[i];
    }
    
    public void setPressures(Pressure[] ListOfPressures) {
    	pressures = ListOfPressures;
    }
    
    public void setRateCoefficients(ArrheniusKinetics[] ListOfKinetics) {
    	kinetics = ListOfKinetics;
    }
    
    public Pressure getPressure(int i) {
    	return pressures[i];
    }

}
