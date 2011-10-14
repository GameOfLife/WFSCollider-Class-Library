// a quick hack to get around the NodeWatcher bug in current sc

NodeActionKeeper {
	classvar <>freeDict, <>startDict;
	*initClass { 
		freeDict = IdentityDictionary();
		startDict = IdentityDictionary();
	}
}



+ Node {
	freeAction2_ { |action| // performs action once and then removes it
		if( NodeActionKeeper.freeDict[ this ].notNil ) {
			NodeActionKeeper.freeDict[ this ].remove;
		};
		
		NodeActionKeeper.freeDict[ this ] = OSCresponderNode( this.server.addr,
			'/n_end', { |time, resp, msg|
				if( msg[1] == this.nodeID ) {
					action.value( this );
					resp.remove;
					if( NodeActionKeeper.freeDict[ this ] == resp ) {
						NodeActionKeeper.freeDict[ this ] = nil;
					};
				};
			}
		).add;	
	}
	
	startAction2_ { |action| // performs action once and then removes it
		if( NodeActionKeeper.startDict[ this ].notNil ) {
			NodeActionKeeper.startDict[ this ].remove;
		};
		
		NodeActionKeeper.startDict[ this ] = OSCresponderNode( this.server.addr,
			'/n_go', { |time, resp, msg|
				if( msg[1] == this.nodeID ) {
					action.value( this );
					resp.remove;
					if( NodeActionKeeper.startDict[ this ] == resp ) {
						NodeActionKeeper.startDict[ this ] = nil;
					};
				};
			}
		).add;	
	}
}