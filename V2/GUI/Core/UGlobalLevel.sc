/*
UGlobalLevel automatically creates an equaliser for U (unit), which is controlled via a global setting.

It creates the following private controls automatically:

	u_globalGain (-12) : the global level for all U's that have this in their path

UGlobalEQ.gui creates a gui for the global EQ. It sends its values to the rootnode of each
server in the UServerCenter, so that all currently active units get the correct settings.
 
*/

UGlobalGain {
	
	classvar <gain = -12;
	classvar <>view;
	
	*initClass {
		Class.initClassTree(ControlSpec);

		
		ControlSpec.specs = ControlSpec.specs.addAll([
			\u_globalGain -> UGlobalGainSpec
		]);
	}
	
	*gain_ { |new|
		gain = (new ? gain);
		this.update;
	}
	
	*kr { |use = 1|
		^(\u_globalGain.kr( gain ) * use).dbamp;	
	}
	
	*gui { |parent, bounds|
		if( view.isNil or: { view.view.isClosed } ) {
			bounds = bounds ?? { 
				Rect( 
					Window.screenBounds.width - 100, 
					0, 
					100, 
					Window.screenBounds.height - 150 
			); };
			view = EZSmoothSlider( nil, bounds,
				controlSpec: [ -60, 36, \lin, 1, -12, "db" ],
				labelHeight: 50
			).value_( gain ).action_({ |vw| this.gain = vw.value });
			view.view.resize_(5);
			view.numberView.autoScale_( true ).scroll_step_( 1 );
			view.sliderView.mode_( \move );
		} {
			^view.front;
		};
	}
	
	*asUGenInput { ^gain.asUGenInput }
	*asControlInput { ^gain.asControlInput }
	*asOSCArgEmbeddedArray { | array| ^gain.asOSCArgEmbeddedArray(array) }
	
	*update { |obj, what ... args|
		if( view.isNil or: { view.view.isClosed } ) {
			view.value = gain;
		};
		UServerCenter.servers.do({ |item|
			if( item.class.name == 'LoadBalancer' ) {
				item.servers.do({ |srv|
					this.sendServer( srv );
				});
			} {
				this.sendServer( item );
			};	
		});
	}
	
	*sendServer { |server|
		RootNode( server ).set( \u_globalGain, this );
	}
	
}


UGlobalGainSpec : Spec {
	
	// fixed output: 
	*new { ^this } // only use as class
	
	*asSpec { ^this }
	
	*constrain { ^UGlobalGain } // whatever comes in; UGlobalEQ comes out
	
	*default {  ^UGlobalGain }
	
	*massEditSpec { ^nil }
}
