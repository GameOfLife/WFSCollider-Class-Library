// wslib 2010

// creates a function of which the args can be changed and kept
// functions are stored in a named bank of defs: TransformDef.all

/*
TransformDef( \add, { |in, mul = 1, add = 0| (in*mul) + add }); // create and store a def

x = Transform( \add ); // create a transform

x.values; // -> [1,0]
x.argNames; // -> [ mul, add ]

x.value( 10 ); // -> 10 (func is bypassed when no arguments are changed)

x.add = 5; // change argument

x.value( 10 ); // -> 15

x.value( 10, 2 ); // -> 25 (change argument on the fly)

// it also sends a 'changed' message at each argument change

x.doOnChange( \add, { |tr, key, value| [ key, value].postln }, false );

x.add = 7; // -> [ add, 7 ]

x.def.setSpec( \mul, [0,2]);   // define a controlspec for 'mul' parameter
x.def.setSpec( \add, [-10,10]); 

y = x.gui; // a TransformGUI

y.action = { x.value( 10 ).postln };

x.add = 5; // watch the slider move

(
// a styled gui in user-defined window
// watch the slider move in both windows
w = Window( "x", Rect( 300,25,200,200 ) ).front;
w.addFlowLayout;
RoundView.useWithSkin( ( 
	labelWidth: 40, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.25)
), { 
	z = x.gui( w ); 
});
)

*/

TransformDef : GenericDef {
	
	classvar <>all;
	
	var <>func, <>category;
	var <>bypassFunc;
	
	*new { |name, func, args, category|
		^super.new( name, args ).init( func ).category_( category ? \default );
	}
	
	init { |inFunc|
		func = inFunc ? this.class.defaultFunc;
		argSpecs = ArgSpec.fromFunc( func, argSpecs )[1..]; // leave out the first arg 
		bypassFunc = { |tr| tr.values == tr.def.values }; // if default values
	} 
	
	*defaultFunc {
		^{ |in, mul = 1, add = 0| 
			(in * mul) + add;
		};
	}
	
	*allNamesForCategory { |category = \default|
		var array;
		this.class.all.keysValuesDo({ |key, value| 
			if( value.category == category ) {
				array = array.add( key );
			};
		});
		^array;
	}
	
	*allCategories { 
		var categories = Set();
		this.class.all.do({ |item| categories.add( item.category ) });
		^categories.asArray.sort;
	}
	
}


Transform : ObjectWithArgs {
	
	var <def, <>makeCopy = false;
	
	*new { |defName, args|
		^super.new.init( defName, args ? [] );
	}
	
	*defClass { ^TransformDef }
	
	init { |inName, inArgs|
		def = this.class.defClass.fromName( inName );
		if( def.notNil ) {	
			args = def.asArgsArray( inArgs ); // create ordered array of args
		} { 
			"defName '%' not found".format(inName).warn; 
		};
	}
	
	value { |input ...inArgs| // in place by default
		this.values = inArgs;
		if( def.bypassFunc.value( this ).not ) { 
			if( makeCopy) { input = input.copyNew };
			^this.prValue( input, *inArgs ); 
		} {
			^input;
		};
	}
	
	prValue { |input|
		^def.func.value( input, *(this.values.collect(_.value)) );
	}	
	
	defName_ { |name, keepArgs = true|
	  	this.init( name.asSymbol, if( keepArgs ) { args } { [] }); // keep args
	}
	
	reset { this.values = this.def.values.deepCopy; }
	
	defName { ^def.name }
}

