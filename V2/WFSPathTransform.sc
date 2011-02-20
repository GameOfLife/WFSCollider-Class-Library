/*
// example:

p = WFSPath.circle; // create a WFSPath

WFSPathTransformDef( \scale, { |path, x = 1, y = 1| 
	path.positions = path.positions.collect({ |item| item * (x@y) });
});

x = WFSPathTransform( \scale ).makeCopy_( true ); // make a copy of the transformed event

x.value( p, (..6), 2, 2 ).plotSmooth; // performs on selection

x.def.setSpec( \x, [-10,10]); // set the specs
x.def.setSpec( \y, [-10,10]);

y = x.gui; // create a gui (a TransformGUI)

y.action = { x.value( p ).plotSmooth; }; // action for the gui
*/

WFSPathTransformDef : TransformDef {
	classvar <>all;
	
	var <>useSelection = true;
	
	*defaultFunc { 
		^{ |path, mul = 1, add = 0| 
			path.positions = path.positions.collect({ |pos,i| (pos * mul) + add; });
		};
	}

	
}

WFSPathTransform : Transform {
	
	*defClass { ^WFSPathTransformDef }
	
	value { |path, selection ...inArgs|
		this.values = inArgs;
		if( def.bypassFunc.value( this ).not ) { 
			if( makeCopy ) { path = path.copyNew };
			if( def.useSelection ) { 
				^this.prValueSelection( path, selection );
			} {
				^this.prValue( path ); 
			};
		} {
			^path;
		};
	}
	
	prValueSelection { |path, selection|
		var result;
		result = this.prValue( path.copySelection( selection ) );
		path.putSelection( selection, result );
		^path;
	}
	
	prValue { |path| // no selection
		^def.func.value( path, *this.values );
	}	
	
}


+ WFSPath {
	
	copySelection { |indices, newName = "selection" | // indices should not be out of range!
		var pos, tim;
		indices = indices ?? { (..positions.size-1) };
		pos = positions[ indices ].collect(_.copy);
		tim = times[ indices[ 0..indices.size-2 ] ];
		^this.class.new( pos, tim, newName );
	}
	
	putSelection { |indices, selectionPath| // in place operation !!
		selectionPath = selectionPath.asWFSPath; 
		indices = indices ?? { (..selectionPath.positions.size-1) };
		indices.do({ |item, i|
			positions.put( item, selectionPath.positions[i].copy );
			if( i < selectionPath.times.size ) { times.put( item, selectionPath.times[i] ) };
		});	
	}	
}