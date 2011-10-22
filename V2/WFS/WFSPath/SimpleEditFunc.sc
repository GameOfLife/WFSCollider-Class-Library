SimpleEditDef : GenericDef {
	
	classvar <>all;
	
	var <>func;	
	var >defaults;
	var <>makeViewsFunc;
	var <>postMakeViewsFunc;
	var >bypassFunc;
	var <>viewHeight = 14;
	
	*new { |name, func, args, defaults|
		^super.new( name, args ).init( func, defaults );
	}
	
	*getFromFile { ^nil; } // file write/read not supported for now
	
	init { |inFunc, inDefaults|
		defaults = inDefaults ? defaults;
		func = inFunc ? func;
	}
	
	defaultBypassFunc { ^{ |f, obj| f.defaults( obj ) == f.args }; }
	
	bypassFunc { ^bypassFunc ? this.defaultBypassFunc }
	
	checkBypass { |f, obj| ^this.bypassFunc.value( f, obj ) == true  }
	
	defaults { |f, obj|
		^defaults !? { defaults.value( f, obj ) } ?? { this.args };
	}
	
	makeViews { |parent, bounds, f|
		var res;
		RoundView.useWithSkin( ( 	
				font: Font( Font.defaultSansFace, 10 ),
				labelWidth: 55
		) ++ (RoundView.skin ? ()), {
				if( makeViewsFunc.notNil ) {
					res = makeViewsFunc.value( parent, bounds, f )
				} {
					res = this.prMakeViews( parent, bounds, f );
				};
				postMakeViewsFunc.value( f, res );
			 }
		);
		^res;
	}
	
	viewNumLines {
		^this.specs.collect({|spec|
			if( spec.isNil ) {
				1
			} {
				spec.viewNumLines
			};
		}).sum;
	}
	
	getHeight { |margin, gap|
		viewHeight = viewHeight ? 14;
		margin = margin ?? {0@0};
		gap = gap ?? {4@4};
		^(margin.y * 2) + ( this.viewNumLines * (viewHeight + gap.y) ) - gap.y;
	}
		
	
	prMakeViews { |parent, bounds, f|
		var views, controller, composite;
		var margin = 0@2, gap = 0@0;
		
		if( parent.isNil ) {
			bounds = bounds ?? { 160 @ this.getHeight( margin, gap ) };
		} {
			bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
			bounds.height = this.getHeight( margin, gap );
		};
		
		controller = SimpleController( f );
		
		composite = EZCompositeView( parent, bounds, true, margin, gap ).resize_(2);
		bounds = composite.view.bounds;
		composite.onClose = {
			controller.remove
		 };
		
		views = ();
		
		views[ \composite ] = composite;
		
		f.args.pairsDo({ |key, value, i|
			var vw, spec;
			
			spec = this.specs[i/2];
			
			vw = ObjectView( composite, nil, f, key, spec, controller );
				
			vw.action = { f.action.value( f, key, value ); };
				
			views[ key ] = vw;
		
		});
		
		if( views.size == 0 ) {
			controller.remove;
		};
		
		^views;
	}
	
}

SimpleEdit : ObjectWithArgs {
	
	var <>action;
	var <defName;
	var <>makeCopy = false;
	
	*new { |defName, args|
		^super.new.init( defName, args ? [] )
	}
	
	*defClass { ^SimpleEditDef }
	
	init { |inName, inArgs|
		var def;
		if( inName.isKindOf( this.class.defClass ) ) {
			def = inName;
			defName = def.name;
			if( defName.isNil ) { defName = def };
		} {
			defName = inName;
			def = this.class.defClass.fromName( defName );
		};
		if( def.notNil ) {	
			args = def.asArgsArray( inArgs ? [] );
			// defName = def.name;
		} { 
			//defName = inName;
			"% defName '%' not found".format(this.class.defClass, inName).warn; 
		};
		this.changed( \init );
	}
	
	def { 
		if( defName.isKindOf( this.class.defClass ) ) {
			^defName
		} {
			^this.class.defClass.fromName( defName );
		};
	}
	
	defaults { |obj| ^this.def.defaults( this, obj ); }
	
	getSpec { |key| ^this.def.getSpec( key ) }
	
	set { |argName, value, constrain = false|
		var spec;
		if( constrain && { (spec = this.getSpec( argName )).notNil } ) { 
			value = spec.constrain( value );
		};
		this.setArg( argName, value );
	}
	
	get { |key|
		^this.getArg( key );
	}
	
	args_ { |newArgs, constrain = false|
		newArgs.pairsDo({ |argName, value|
			this.set( argName, value, constrain );
		});
	}
	
	doesNotUnderstand { |selector ...args| 
		if( selector.isSetter ) { 
			this.set( selector.asGetter, *args ) 
		} {
			^this.get( selector );
		};	
	}
	
	reset { |obj| // can use an object to get the defaults from
		this.args = this.defaults( obj );
	}
	
	checkBypass { |obj| ^this.def.checkBypass( this, obj ) }
	
	value { |obj, args|
		this.args = args;
		if( makeCopy ) { obj = obj.deepCopy };
		if( this.checkBypass( obj ) ) {
			^obj;
		} {
			^this.prValue( obj );
		};

	}
	
	prValue { |obj|
		^this.def.func.value( this, obj );
	}
	
	viewNumLines { ^this.def.viewNumLines }
	
	makeViews { |parent, bounds|
		^this.def.makeViews( parent, bounds, this );
	}
}