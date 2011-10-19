WFSPathEditFunc : SimpleEditFunc {
	
	var <>useSelection = true;
	var <>selection;
	
	prValue { |obj|
		if( useSelection && selection.notNil ) {
			this.prValueSelection( obj )
		} {
			^func.value( this, obj );
		};
	}
	
	prValueSelection { |obj|
		var result;
		result = func.value( this, obj.copySelection( selection ) );
		obj.putSelection( selection, result );
		^obj;
	}	
	
}