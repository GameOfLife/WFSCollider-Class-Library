+ Object {
	
	sublassMethodExistsFor { |selector|
		^this.class.findRespondingMethodFor( selector ) != 
			Object.findRespondingMethodFor( selector )
	}
	
	prettyPrintIndent { |indent = 0|
		^String.fill( indent, $\t );
	}
	
	prettyPrintOn { |stream, indent = 0|
		if( this.sublassMethodExistsFor( \storeOn ) ) {
			stream << this.prettyPrintIndent( indent );
			this.storeOn( stream ); // use subclass method instead
		} {
			stream << this.prettyPrintIndent( indent ) << this.class.name;
			this.prettyPrintParamsOn( stream, indent );
			this.prettyPrintModifiersOn( stream, indent );
		};
	}
	
	prettyPrintParamsOn { arg stream, indent = 0;
		var args = this.storeArgs;
		if(args.notEmpty) {
			args = this.simplifyStoreArgs( args );
			stream << "(";
			args.prettyPrintItemsOn( stream, indent + 1 );
			stream  << ")";
		} {
			stream << ".new"
		}
	}
	
	prettyPrintModifiersOn { arg stream;
		this.storeModifiersOn( stream );
	}
	
	asPrettyString { |indent = 0|
		^String.streamContents({ arg stream; this.prettyPrintOn(stream, indent); });
	}
	
}

+ Collection {
	
	prettyPrintOn { | stream, indent |
		if (stream.atLimit) { ^this };
		stream << this.prettyPrintIndent( indent ) << this.class.name << "[ " ;
		this.prettyPrintItemsOn(stream, indent + 1);
		stream << "]" ;
	}
	
	prettyPrintGetItems { |indent = 0|
		^this.collect { | item |
			item.asPrettyString( indent )[indent..];
		};
	}
	
	prettyPrintItemsOn { | stream, indent = 0 |
		var items, indentString;
		items = this.prettyPrintGetItems( indent );
		if( items.any( _.includes( $\n ) ) or: { 
			items.collect({ |item| item.size + 2 }).sum > 80
		}) {
			indentString = this.prettyPrintIndent( indent );
			stream << "\n" << items.collect({ |item|
				indentString ++ item;
			}).join( ",\n" ) << "\n" << this.prettyPrintIndent( indent - 1 );
		} {
			stream << items.join( ", " ) << " ";
		};
	}
	
}

+ Array {
	prettyPrintOn { | stream, indent |
		if (stream.atLimit) { ^this };
		stream << this.prettyPrintIndent( indent ) << "[ ";
		this.prettyPrintItemsOn(stream, indent + 1);
		stream << "]" ;
	}
}
