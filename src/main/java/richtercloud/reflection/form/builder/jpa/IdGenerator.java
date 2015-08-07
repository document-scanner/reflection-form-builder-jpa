/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package richtercloud.reflection.form.builder.jpa;

/**
 *
 * @author richter
 */
public class IdGenerator {
    private final static IdGenerator instance = new IdGenerator();

    public static IdGenerator getInstance() {
        return instance;
    }
    private long nextId = 0;

    protected IdGenerator() {
    }
    
    public Long getNextId() {
        this.nextId += 1;
        return nextId;
    }
}
