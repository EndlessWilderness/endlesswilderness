package com.jme3.bullet.control;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;

public class MMOCharacterControl extends BetterCharacterControl {
    
    private static final Logger log = LoggerFactory.getLogger(MMOCharacterControl.class);
    
    private boolean jumping = false;

    public MMOCharacterControl(float radiusIn, float heightIn, float massIn) {
        super(radiusIn, heightIn, massIn);
    }

    public float getMass() {
        return this.rigidBody.getMass();
    }

    public void jump() {
        if (this.onGround) {
            this.jump = true;
            log.debug("Attempting to Jump");
        } else {
            log.debug("Jump failed - Not on Ground");
        }
    }
    
    public boolean isJumping() {
        return this.jumping;
    }

    @SuppressWarnings("unchecked")
    protected void checkOnGround() {
        TempVars vars = TempVars.get();
        Vector3f location = vars.vect1;
        Vector3f rayVector = vars.vect2;
        float height = getFinalHeight();
        location.set(localUp).multLocal(height).addLocal(this.location);
        rayVector.set(localUp).multLocal(-(height + (FastMath.PI * radius * 1.5f))).addLocal(location);
        List<PhysicsRayTestResult> results = space.rayTest(location, rayVector);
        vars.release();
        for (PhysicsRayTestResult physicsRayTestResult : results) {
            if (!physicsRayTestResult.getCollisionObject().equals(rigidBody)) {
                if (!this.onGround) {
                    log.debug("Landing - " + this.toString());
                    this.jumping = false;
                }
                this.onGround = true;
                return;
            }
        }
        if (log.isDebugEnabled() && this.onGround) {
            log.debug("Going airborne - " + this.toString());
        }
        this.onGround = false;
    }
    
    /**
     * Used internally, don't call manually
     *
     * @param space
     * @param tpf
     */
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        checkOnGround();
        if (wantToUnDuck && checkCanUnDuck()) {
            setHeightPercent(1);
            wantToUnDuck = false;
            ducked = false;
        }
        TempVars vars = TempVars.get();

        // dampen existing x/z forces
        float existingLeftVelocity = velocity.dot(localLeft);
        float existingForwardVelocity = velocity.dot(localForward);
        Vector3f counter = vars.vect1;
        existingLeftVelocity = existingLeftVelocity * physicsDamping;
        existingForwardVelocity = existingForwardVelocity * physicsDamping;
        counter.set(-existingLeftVelocity, 0, -existingForwardVelocity);
        localForwardRotation.multLocal(counter);
        velocity.addLocal(counter);

        float designatedVelocity = walkDirection.length();
        if (designatedVelocity > 0) {
            Vector3f localWalkDirection = vars.vect1;
            // normalize walkdirection
            localWalkDirection.set(walkDirection).normalizeLocal();
            // check for the existing velocity in the desired direction
            float existingVelocity = velocity.dot(localWalkDirection);
            // calculate the final velocity in the desired direction
            float finalVelocity = designatedVelocity - existingVelocity;
            localWalkDirection.multLocal(finalVelocity);
            // add resulting vector to existing velocity
            velocity.addLocal(localWalkDirection);
        }
        rigidBody.setLinearVelocity(velocity);
        if (jump) {
            // TODO: precalculate jump force
            Vector3f rotatedJumpForce = vars.vect1;
            rotatedJumpForce.set(jumpForce);
            rigidBody.applyImpulse(localForwardRotation.multLocal(rotatedJumpForce), Vector3f.ZERO);
            jumping = true;
            jump = false;
        }
        vars.release();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("L: ");
        sb.append(this.location);
        sb.append(" V: ");
        sb.append(this.getVelocity());
        if (this.onGround) {
            sb.append(" - Grounded - ");
        } else {
            sb.append(" - Airborne - ");
        }
        sb.append("W: ");
        sb.append(this.getWalkDirection());
        sb.append(" J: ");
        sb.append(this.isJumping());
        return sb.toString();
    }
}
