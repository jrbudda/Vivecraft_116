package org.vivecraft.render.ik;

// StellaArtois 2014
public class IKHelper
{
    /******************************************************************************
     Copyright (c) 2008-2009 Ryan Juckett
     http://www.ryanjuckett.com/

     This software is provided 'as-is', without any express or implied
     warranty. In no event will the authors be held liable for any damages
     arising from the use of this software.

     Permission is granted to anyone to use this software for any purpose,
     including commercial applications, and to alter it and redistribute it
     freely, subject to the following restrictions:

     1. The origin of this software must not be misrepresented; you must not
     claim that you wrote the original software. If you use this software
     in a product, an acknowledgment in the product documentation would be
     appreciated but is not required.

     2. Altered source versions must be plainly marked as such, and must not be
     misrepresented as being the original software.

     3. This notice may not be removed or altered from any source
     distribution.
     ******************************************************************************/
    ///***************************************************************************************
    /// CalcIK_2D_TwoBoneAnalytic
    /// Given a two bone chain located at the origin (bone1 is the parent of bone2), this
    /// function will compute the bone angles needed for the end of the chain to line up
    /// with a target position. If there is no valid solution, the angles will be set to
    /// get as close to the target as possible.
    ///
    /// Slightly altered in syntax and object use for Java port, StellaArtois 2014
    ///
    /// returns: IKInfo object. The IKInfo.foundValidSolution flag is true if a valid solution was
    ///          found.
    ///***************************************************************************************
    public static IKInfo CalcIK_2D_TwoBoneAnalytic
    (
            boolean solvePosAngle2, // Solve for positive angle 2 instead of negative angle 2
            double length1,      // Length of bone 1. Assumed to be >= zero
            double length2,      // Length of bone 2. Assumed to be >= zero
            double targetX,      // Target x position for the bones to reach
            double targetY       // Target y position for the bones to reach
    )
    {
        final double epsilon = 0.0001; // used to prevent division by small numbers

        IKInfo ikInfo = new IKInfo();
        ikInfo.foundValidSolution = true;

        double targetDistSqr = (targetX*targetX + targetY*targetY);

        //===
        // Compute a new value for angle2 along with its cosine
        double sinAngle2;
        double cosAngle2;

        double cosAngle2_denom = 2*length1*length2;
        if( cosAngle2_denom > epsilon )
        {
            cosAngle2 =   (targetDistSqr - length1*length1 - length2*length2)
                    / (cosAngle2_denom);

            // if our result is not in the legal cosine range, we can not find a
            // legal solution for the target
            if( (cosAngle2 < -1.0) || (cosAngle2 > 1.0) )
                ikInfo.foundValidSolution = false;

            // clamp our value into range so we can calculate the best
            // solution when there are no valid ones
            cosAngle2 = Math.max(-1, Math.min(1, cosAngle2));

            // compute a new value for angle2
            ikInfo.angle2 = Math.acos(cosAngle2);

            // adjust for the desired bend direction
            if( !solvePosAngle2 )
                ikInfo.angle2 = -ikInfo.angle2;

            // compute the sine of our angle
            sinAngle2 = Math.sin(ikInfo.angle2);
        }
        else
        {
            // At least one of the bones had a zero length. This means our
            // solvable domain is a circle around the origin with a radius
            // equal to the sum of our bone lengths.
            double totalLenSqr = (length1 + length2) * (length1 + length2);
            if(    targetDistSqr < (totalLenSqr-epsilon)
                    || targetDistSqr > (totalLenSqr+epsilon) )
            {
                ikInfo.foundValidSolution = false;
            }

            // Only the value of angle1 matters at this point. We can just
            // set angle2 to zero.
            ikInfo.angle2    = 0.0;
            cosAngle2 = 1.0;
            sinAngle2 = 0.0;
        }

        //===
        // Compute the value of angle1 based on the sine and cosine of angle2
        double triAdjacent = length1 + length2*cosAngle2;
        double triOpposite = length2*sinAngle2;

        double tanY = targetY*triAdjacent - targetX*triOpposite;
        double tanX = targetX*triAdjacent + targetY*triOpposite;

        // Note that it is safe to call Atan2(0,0) which will happen if targetX and
        // targetY are zero
        ikInfo.angle1 = Math.atan2(tanY, tanX);

        return ikInfo;
    }
}


