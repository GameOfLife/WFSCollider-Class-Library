GlobalPathDict {
	
	classvar <>dict, <>replaceChar = $@;
	
	*initClass { dict = IdentityDictionary(); }
	
	*put { |key, path|
		dict.put( key, path );
	}
	
	*at { |key|
		^dict.at( key );
	}
	
	*getPath { |path|
		var index, key;
		if( path[0] == replaceChar ) {
			index = path.indexOf( $/ ) ?? { path.size};
			key = path[1..index-1].asSymbol;
			path = path[index+1..];
			if( this.at( key ).isNil) {
				"%:getPath - % not found"
					.format( this, key )
					.warn;
				^path.standardizePath;
			} {
				^(this.at( key ).withoutTrailingSlash +/+ path).standardizePath;
			};
		} {
			^path.standardizePath;
		};
	}
	
	*formatPath { |path|
		var stPath, array = [], key, i = 0;
		
		dict.keysValuesDo({ |key, value|
			array = array.add( [ value.standardizePath.withTrailingSlash, key ] );
		});
		
		array = array.sort({ |a,b|
			a[0].size <= b[0].size;
		}).reverse;
		
		stPath = this.getPath( path );
		
		while { key.isNil && (i < array.size) } {
			if( stPath.find( array[i][0] ) == 0 ) {
				key = array[i][1];
			} {
				i = i+1;
			};
		};
		
		if( key.notNil ) {
			^stPath.findReplace( array[i][0], replaceChar ++ key ++ "/" );
		} {
			^path;
		};
	}
}

+ String {
	
	getGPath {
		^GlobalPathDict.getPath( this );
	}
	
	formatGPath {
		^GlobalPathDict.formatPath( this );
	}
	
	putGPath { |key = \default|
		GlobalPathDict.put( key, this );
	}
	
}

+ Nil {
	
	putGPath { |key = \default|
		GlobalPathDict.put( key, this );
	}
	
	getGPath { ^this }
	
	formatGPath { ^this }

}