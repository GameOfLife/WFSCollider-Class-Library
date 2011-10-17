/*
WFSPathURL manages file locations for WFSPath2. It makes sure only one url is
associated with a specific path. If the url is changed it makes a copy of the path 
for on the old url, and if a path was already on an url it moves it into "orphaned".

WFSPathURL behaves like a WFSPath2 in all other 

*/

WFSPathURL {
	
	classvar <>all;
	classvar <>orphaned;
	
	var url;
	
	*initClass { 
		all = IdentityDictionary(); 
		
	}
	
	*new { |url|
		url = this.formatURL( url );
		^this.newCopyArgs( url ).init;
	}
	
	init {
		var wfsPath;
		wfsPath = this.class.getWFSPath( url );
		if( wfsPath.isNil ) {
			WFSPath2.read( url ); // can return nil if file not available
		};
		this.changed( \init );
	}
	
	*formatURL { |url|
		if( url.notNil ) {
			^url.asString.formatGPath.asSymbol;
		} {
			^nil;
		};
	}
	
	*getWFSPath { |url|
		url = this.formatURL( url );
		^all[ url ];
	}
	
	*getURL { |wfsPath|
		all.keysValuesDo({ |key, value|
			if( value === wfsPath ) { ^key.asString };
		});
		^nil;
	}
	
	*putWFSPath { |url, wfsPath, keepOld = true|
		var oldPath, oldURL;
		if( keepOld ) {
			if( ( oldPath = this.getWFSPath( url ) ).notNil ) {
				orphaned = orphaned.add( oldPath );
			};
		};
		if( ( oldURL = this.getURL( wfsPath ) ).notNil ) {
			all.put( oldURL.asSymbol, wfsPath.deepCopy );
		};
		all.put( this.formatURL( url ), wfsPath );
	}
	
	wfsPath {
		^all[ url ];
	}
	
	url_ { |url|
		url = this.class.formatURL( url );
		this.init;
	}
	
	url { ^url.asString }
	
	
	doesNotUnderstand { |selector ...args|
		var res, wfsPath;
		wfsPath = this.wfsPath;
		if( wfsPath.isNil ) {
			^nil;
		} {
			res = wfsPath.perform( selector, *args );
			if( res === wfsPath ) {
				^this;
			} {
				^res;
			};
		};
	}
	
	exists { this.wfsPath.notNil }
	
	isWFSPath2 { ^true }
	
	storeArgs { ^[ url.asString ] }
	
}