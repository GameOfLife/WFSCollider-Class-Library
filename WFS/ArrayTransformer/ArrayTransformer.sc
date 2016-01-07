ArrayTransformer : SimpleTransformer {
	
	var <selection;
	var <spec;
	var <>mappedArgs;
	
	*defClass { ^ArrayTransformerDef }
	
	prValue { |obj|
		var def;
		def = this.def;
		obj = obj.asCollection;
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
		var result, size, sel;
		size = obj.size;
		sel = selection.select({ |item| item < size });
		result = this.applyFunc( obj[ sel ], def );
		sel.do({ |index, i|
			obj.put( index, result[i] );
		});
		^obj;
	}
	
	selection_ { |newSelection| selection = newSelection; this.changed( \selection, selection ); }
}

ArrayGenerator : ArrayTransformer {
	
	
	// \bypass, \replace, \+, \-, \*, <any binary operator>
	var <mode = \replace;
	
	var <blend = 1; // 0 to 1
	
	*defClass { ^ArrayGeneratorDef }
	
	applyFunc { |obj, def|
		var result, size;
		def = def ?? { this.def };
		size = obj.size;
		result = def.func.value( this, size, obj );
		^switch( mode,
			\replace, { 
				obj.blend( result, blend );
			},
			\lin_xfade, {
				obj.blend( result, blend * ((..size-1)/(size-1)) );
			},
			\bypass, { obj },
			{
				result = result.perform( mode, obj );
				obj.blend( result, blend );
			}
		);
	}
	
	reset { |obj, all = false| // can use an object to get the defaults from
		this.blend = 0;
		//if( all == true ) {
			this.args = this.defaults( obj );
		//};
	}
	
	mode_ { |newMode| mode = newMode; this.changed( \mode, mode )  }
	blend_ { |val = 1| blend = val; this.changed( \blend, blend )  }
	
	storeModifiersOn{|stream|
		if( blend != 1 ) {
			stream << ".blend_(" <<< blend << ")";
		};
		if( mode !== \replace ) {
			stream << ".mode_(" <<< mode << ")";
		};
	}
}

ArrayTransformerDef : SimpleTransformerDef {
	classvar <>all;
	classvar <>defsFolders, <>userDefsFolder;
	
	var <>useSelection = true;
	
	*initClass{
		defsFolders = [ 
			this.filenameSymbol.asString.dirname +/+ "ArrayTransformerDefs"
		];
		userDefsFolder = Platform.userAppSupportDir ++ "/ArrayTransformerDefs/";
	}
	
	objectClass { ^ArrayTransformer }
}

ArrayGeneratorDef : ArrayTransformerDef {
	
	classvar <>defsFolders, <>userDefsFolder;
	
	*initClass{
		defsFolders = [ 
			this.filenameSymbol.asString.dirname +/+ "ArrayGeneratorDefs"
		];
		userDefsFolder = Platform.userAppSupportDir ++ "/ArrayGeneratorDefs/";
	}
	
	defaultBypassFunc { 
		^{ |f, obj|
			(f.blend == 0) or: { f.mode === \bypass };
		};
	}
	
	objectClass { ^ArrayGenerator }	

}