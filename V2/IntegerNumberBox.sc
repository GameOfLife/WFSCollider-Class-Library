IntegerNumberBox : RoundNumberBox {
	
	*new { arg ...args;
		^super.new( *args ).initIntegerNumberBox
	}
	
	initIntegerNumberBox{
		allowedChars = "";
		step = 1;
		clipLo = 0;
	}
	
	
}