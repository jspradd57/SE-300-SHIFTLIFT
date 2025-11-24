package se300.shiftlift;

/**
 * Custom annotation for discriminator column configuration.
 * Used to specify the column name for entity type discrimination.
 */
public @interface DescriminatorColumn {

    /**
     * Specifies the name of the discriminator column.
     * 
     * @return the column name
     */
    public String name();

}
