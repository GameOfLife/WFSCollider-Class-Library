/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSSpeaker : WFSPoint {

	var <>radianAngle;
	
	// angle is stored internally as radian for optimization reasons
	
	*new { arg x=0, y=0, z=0, angle=0;
		^super.newCopyArgs(x,y,z, WFSTools.asRadians(angle) );
		}
	
	*newRadian {  arg x=0, y=0, z=0, radianAngle=0;
		^super.newCopyArgs(x,y,z, radianAngle );
		}
		
	*newAZ { arg angleFromCenter = 0, distFromCenter = 0, emissionAngle, z = 0; 
		var x, y, radianAngle;
		#x, y = WFSTools.singleAZToXY( angleFromCenter, distFromCenter );
		if( emissionAngle.isNil ) 
			{ radianAngle = (WFSTools.asRadians( angleFromCenter ) - pi) % 2pi }
			{ radianAngle = WFSTools.asRadians( emissionAngle ) };
		^super.newCopyArgs( x, y, z, radianAngle );
		}
		
	angleOffset { |aWFSPoint| // relative to speaker direction
		^WFSTools.fromRadians( this.radianAngleOffset( aWFSPoint ) );
		}
	
	angle { ^WFSTools.fromRadians( radianAngle ); }
	angle_ { |newAngle| radianAngle = WFSTools.asRadians( newAngle ); } 
		
	radianAngleOffset {  |wfsPoint=0|
		wfsPoint = wfsPoint.asWFSPoint;
		^radianAngle - wfsPoint.radianAngleFrom( this );
		}
		
	pointIsBehindBasic { |wfsPoint|
		//wfsPoint = wfsPoint.asWFSPoint;
		^(this.radianAngleOffset(wfsPoint) %2pi).inRange( 0.5pi, 1.5pi);
		}
		
	pointIsBehind { |wfsPoint=0| // optimized for rectangular speaker setups
		var localAngle;
		wfsPoint = wfsPoint.asWFSPoint;
		
		if( WFSPath.azMax != 360 )
			{ localAngle = (this.angle / WFSPath.azMax) * 360; }
			{ localAngle = this.angle };
		
		case { localAngle == 0 }
			{ ^wfsPoint.y < y	}
			{ localAngle == 180 }
			{ ^wfsPoint.y > y	}
			{ localAngle == 90 }
			{ ^wfsPoint.x < x }
			{ localAngle == 270 }
			{ ^wfsPoint.x > x }
			
			{ true }
			{ ^this.pointIsBehindBasic( wfsPoint ) };
		}
	
	pointIsInFront { |wfsPoint=0|
		wfsPoint = wfsPoint.asWFSPoint;
		^((this.radianAngleOffset(wfsPoint) - pi ) %2pi).inRange( 0.5pi, 1.5pi);
		}
	
	distDirection { |wfsPoint=0| // -1 when point in front of line, 1 when point behind line or on
		^(this.pointIsBehind( wfsPoint ).binaryValue * 2) - 1
		//^this.radianAngleOffset(wfsPoint).cos.sign.neg;
		}
	
	neg { ^this.class.newRadian( x.neg, y.neg, z.neg, radianAngle ) }
	
	asWFSPoint { ^WFSPoint( x,y,z ) }
	
	asWFSSpeaker { ^this }
	
	isInLineWith { |aSpeaker, round = 1.0e-12|
		aSpeaker = aSpeaker.asWFSSpeaker;
		^(this.isNextTo( aSpeaker, round ) && (radianAngle == aSpeaker.radianAngle) )
		}
		
	printOn { arg stream;
		stream << "WFSSpeaker( " << x << ", " << y << ", " << z << ", " << this.angle << " )";
	}
	
	storeArgs { ^[x,y,z, this.angle] }
	
	}
