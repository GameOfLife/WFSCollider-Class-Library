WFSPathTransformDef : TransformDef {
	classvar <>all;
	
	*initClass {
		this.all = IdentityDictionary();
	}
	
	*defaultFunc { 
		^{ |path, mul = 1, add = 0| 
			path.positions = path.positions.collect({ |pos,i| (pos * mul) + add; });
		};
	}

	
}

WFSPathTransform : Transform {
	
	*defClass { ^WFSPathTransformDef }
	
	prValue { |path, selection|
		^def.func.value( path, selection, *this.values );
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
		indices = indices ?? { (..selectionPath.positios.size-1) };
		indices.do({ |item, i|
			positions.put( item, selectionPath.positions[i].copy );
			if( i < selectionPath.times.size ) { times.put( item, selectionPath.times[i] ) };
		});	
	}	
}