/*
// example

(
// create two WFSUnitDefs

Udef( \sine, { |freq = 440, amp = 0.1|
	UOut.ar( 0, SinOsc.ar( freq, 0, amp ) )
} ).loadSynthDef;

Udef( \vibrato, { |rate = 1, amount = 1.0|
	UOut.ar( 0, SinOsc.ar( rate ).range(1-amount,1) * UIn.ar( 0 ) )
} ).loadSynthDef;

Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] ).loadSynthDef;

)

x = UChain( \sine, \vibrato, \output );

x.units[0].args
x.start;
x.stop;

x.units

(

// a styled gui in user-defined window
// -- to be replaced by WFSChainGUI later -- 

w = Window( "x", Rect( 300,25,200,300 ) ).front;
w.addFlowLayout;
RoundView.useWithSkin( ( 
	labelWidth: 40, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.33)
), { 
	SmoothButton( w, 16@16 )
		.label_( ['power', 'power'] )
		.hiliteColor_( Color.green.alpha_(0.5) )
		.action_( [ { x.start }, { x.stop } ] )
		.value_( (x.groups.size > 0).binaryValue );
	y = x.units.collect({ |item|
		StaticText( w, (w.view.bounds.width - 8)@16 )
			.string_( " " ++ item.defName.asString )
			.font_( RoundView.skin.font.boldVariant )
			.background_( Color.gray(0.8) );
		item.gui( w );
	}); 
});

)

*/

UChain {
	var <>units, <>groups;
	
	*new { |...units|
		^super.newCopyArgs( units.collect(_.asUnit) )
	}

	makeGroupAndSynth { |target|
	    var group = Group( target )
                .startAction_({ |synth|
                    // only add if started (in case this is a bundle)
                    this.changed( \go, group );
                })
                .freeAction_({ |synth|
                    groups.remove( group );
                    this.changed( \end, group );
                });
        groups = groups.add( group );
        this.changed( \start, group );
        units.do( _.makeSynth(group) );
	}

	makeBundle { |targets|
	    ^targets.asCollection.collect{ |target|
	        target.asTarget.server.makeBundle( false, {
                this.makeGroupAndSynth(target)
            })
		}
	}
	
	start { |target, latency|
		var targets, bundles;
		target = target ? Server.default;
		targets = target.asCollection;
		bundles = this.makeBundle( targets );
		targets.do({ |target, i|
			target.asTarget.server.sendBundle( latency, *bundles[i] );
		});
		if( target.size == 0 ) {
			^groups[0]
		} {
			^groups;
		};
	}
	
	free { groups.do(_.free) }
	stop { this.free }

	prepare { |server| units.do(_.prepare(server) ) }

	prepareAndStart{ |server|
	    fork{
	        this.prepare(server);
	        server.asCollection.do{ |s|
	            s.sync;
	        };
	        this.start(server);
	    }
	}

	dispose { units.do( _.dispose ) }
	
	resetGroups { groups = []; } // after unexpected server quit
}