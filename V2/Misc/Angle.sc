// a simple class mainly to display an angle in a compileString

Angle {
	
	var <>value;
	
	*new { |value|
		^super.newCopyArgs( value ? 0);
	}
	
	printOn { |stream|
		stream << case { 
			value == 0 
			} { 
				value 
			} { 
				value == pi 
			} { 
				"pi" 
			} { 
				(value / pi).asString ++ "pi" 
			};
	}
	storeOn { |stream|
		this.printOn( stream );
	}
	
}