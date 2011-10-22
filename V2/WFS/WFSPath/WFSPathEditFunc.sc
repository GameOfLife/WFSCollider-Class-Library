WFSPathEditDef : SimpleEditDef {
	
	classvar <>all;
	
	var <>useSelection = true;
}

WFSPathGeneratorDef : WFSPathEditDef {
	
	classvar <>all;
	
	var <>changesX = true;
	var <>changesY = true;
	var <>changesT = true;
	
	defaultBypassFunc { 
		^{ |f, obj|
			(f.blend > 0) and: {
				(f.modeT === \bypass) and: {
					(f.modeX === \bypass) and:  {
						(f.modeY === \bypass)
					}
				}
			};
		};
	}
}

WFSPathEdit : SimpleEdit {
	
	var <>selection;
	
	*defClass { ^WFSPathEditDef }
	
	prValue { |obj|
		var def;
		def = this.def;
		if( def.useSelection.value( this, obj ) && { selection.size > 0 } ) {
			^this.prValueSelection( obj, def )
		} {
			^this.applyFunc( obj, def );
		};
	}
	
	applyFunc { |obj, def|
		def = def ?? { this.def };
		^def.func.value( this, obj );
	}
	
	prValueSelection { |obj, def|
		var result;
		result = this.applyFunc( obj.copySelection( selection ), def );
		obj.putSelection( selection, result );
		^obj;
	}
	
}


WFSPathGenerator : WFSPathEdit {
	
	// \bypass, \replace, \+, \-, \*, <any binary operator>
	var <>modeX = \replace;
	var <>modeY = \replace;
	var <>modeT = \replace;
	
	var <>blend = 1; // 0 to 1

	var <>polar = false;
	
	*defClass { ^WFSPathGeneratorDef }
	
	applyFunc { |obj, def|
		var copy, result;
		var newX, newY, newT;
		copy = obj.deepCopy;
		def = def ?? { this.def };
		if( polar ) {
			copy.positions = copy.positions.collect({ |item|
				Point( item.rho, item.theta );
			});
			result = def.func.value( this, copy, copy.positions.size );
			result.positions = copy.positions.collect({ |item|
				Polar( item.x, item.y ).asPoint;
			});
		} {
			result = def.func.value( this, copy, copy.positions.size );
		};
		
		if( def.changesX ) {
			newX = result.positions.collect(_.x);
			obj.positions.do({ |item, i|
				var x;
				if( modeX === \replace ) {
					x = newX[i];
				} {
					x = item.x.perform( modeX, newX[i] );
				};
				item.x = item.x.blend( x, blend );
			});
		};
		
		if( def.changesY ) {
			newY = result.positions.collect(_.y);
			obj.positions.do({ |item, i|
				var y;
				if( modeY === \replace ) {
					y = newY[i];
				} {
					y = item.y.perform( modeY, newY[i] );
				};
				item.y = item.y.blend( y, blend );
			});
		};
		
		if( def.changesT ) {
			newT = result.times;
			obj.times = obj.times.collect({ |item, i|
				var t;
				if( modeT === \replace ) {
					t = newT[i];
				} {
					t = item.perform( modeT, newT[i] );
				};
				item.blend( t, blend );
			});
		};
		
		^obj;
	}

}