WFSPositionTracker {
	
	classvar <>all;
	classvar <>positions;
	classvar <sendPointRate = 10;
	classvar <>active = false;
	
	*initClass {
		all = IdentityDictionary();
		positions = IdentityDictionary();
	}
	
	*start {
		UChain.addDependant( this );
		active = true;
	}
	
	*stop {
		UChain.removeDependant( this );
		active = false;
		this.rate = sendPointRate;
		this.clear;
	}
	
	*clear {
		all.do({ |xx| xx.do({ |item| item.remove }); });
		all.clear;
		positions.clear;
	}
	
	*update { |obj, groupDict, mode, uchain|
		switch( mode,
			\add, { this.add( uchain ) },
			\remove, { this.remove( uchain ) }
		); 
	}
	
	*rate_ { |rate = 10|
		sendPointRate = rate;
		Server.all.do({ |srv|
			RootNode(srv).set( \sendPointRate, this.getRate );
		});
	}
	
	*getRate { ^sendPointRate * active.binaryValue }
	
	*add { |uchain|
		var pannerUnits, repliers;
		this.remove( uchain );
		uchain.units.do({ |unit|
			if( [ 
					\wfsStaticPoint, 
					\wfsDynamicPoint, 
					\wfsStaticPlane, 
					\wfsDynamicPlane 
				].includes( unit.name ) ) {
				pannerUnits = pannerUnits.add( unit );
			};
		});
		if( pannerUnits.size > 0 ) {
			repliers = pannerUnits.collect({ |unit, i|
				var synth, unitIndex;
				synth = unit.synths[1]; // prepan synth is always second synth
				unitIndex = uchain.units.indexOf( unit );
				if( synth.notNil ) {
					ReceiveReply( synth, { |point, time, resp, msg|
						positions[ uchain ] !? { 
							if( positions[ uchain ][ unitIndex ].notNil ) {
								positions[ uchain ][ unitIndex ].x = point[0];
								positions[ uchain ][ unitIndex ].y = point[1];
							} {
								positions[ uchain ][ unitIndex ] = point.asPoint;
							};
						};
					}, '/point' );
				} {
					"%:add - no synth found in unit\n".postf( this );
					nil
				};
			}).select(_.notNil);
			if( repliers.size > 0 ) {
				all[ uchain ] = repliers;
				positions[ uchain ] = Order();
			};
		};
	}
	
	*remove { |uchain|
		all[ uchain ].do({ |item| item.remove });
		all[ uchain ] = nil;
		positions[ uchain ] = nil;
	}
	
	*list {
		var objects;
		positions.keysValuesDo({  |uchain, points|
			points.do({ |point, i|
				objects = objects.add( [point, uchain, i] );
			});
		});
		^objects;
	}
	
	*pointsAndLabels {
		var objects;
		positions.keysValuesDo({  |uchain, points|
			points.do({ |point, i|
				objects = objects.add( [point,  uchain.name ++ [ i ].asString ] );
			});
		});
		^(objects ? []).flop;
	}
}