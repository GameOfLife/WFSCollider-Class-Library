/*
UGlobalEQ automatically creates an equaliser for U (unit), which is controlled via a global setting.

It creates the following private controls automatically:

	u_globalEQ_setting (EQSetting) : the setting of the eq
	u_globalEQ_bypass (0) : a bypass option, adjustable per unit

UGlobalEQ.gui creates a gui for the global EQ. It sends its values to the rootnode of each
server in the UServerCenter, so that all currently active units get the correct settings.
 
*/

UGlobalEQ {
	
	classvar <eqSetting;
	classvar <>ctrl;
	classvar <>view;
	
	*initClass {
		Class.initClassTree(EQSetting);
		
		this.eqSetting = EQSetting(); // the default eq setting (may change later)
		
		ControlSpec.specs = ControlSpec.specs.addAll([
			\u_globalEQ_setting -> UGlobalEQSpec,
			\u_globalEQ_bypass -> BoolSpec(false),	
		]);
	}
	
	*eqSetting_ { |new|
		eqSetting.removeDependant( this );
		eqSetting = new;
		eqSetting.addDependant( this );
	}
	
	*ar { |in|
		var setting, bypass;
		setting = \u_globalEQ_setting.kr( eqSetting.asControlInput );
		bypass = \u_globalEQ_bypass.kr( 0 );
		^if( bypass, in, eqSetting.ar( in, setting ) );
	}
	
	*gui { |parent, bounds|
		if( view.isNil or: { view.view.isClosed } ) {
			^view = EQView( parent ? "UGlobalEQ", bounds ? Rect(10, 350, 350, 186), eqSetting )
		} {
			^view.front;
		};
	}
	
	*asUGenInput { ^eqSetting.asUGenInput }
	*asControlInput { ^eqSetting.asControlInput }
	*asOSCArgEmbeddedArray { | array| ^eqSetting.asOSCArgEmbeddedArray(array) }
	
	*update { |obj, what ... args|
		if( what == \setting ) {
			UServerCenter.servers.do({ |item|
				if( item.class.name == 'LoadBalancer' ) {
					item.servers.do({ |srv|
						this.sendServer( srv );
					});
				} {
					this.sendServer( item );
				};	
			});
		};
	}
	
	*sendServer { |server|
		RootNode( server ).set( \u_globalEQ_setting, this );
	}
	
	*doesNotUnderstand { |selector ...args|
		var res;
		res = eqSetting.perform( selector, *args );
		if( res != eqSetting ) { ^res }
	}
	
}


UGlobalEQSpec : Spec {
	
	// fixed output: 
	*new { ^this } // only use as class
	
	*asSpec { ^this }
	
	*constrain { ^UGlobalEQ } // whatever comes in; UGlobalEQ comes out
	
	*default {  ^UGlobalEQ }
	
	*massEditSpec { ^nil }
	
	*findKey {
		^Spec.specs.findKeyForValue(this);
	}
}


