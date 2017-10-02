package org.l2x6.editorconfig.core;

public class FormatException extends RuntimeException
{

    private static final long serialVersionUID = 6658236116388833334L;

    public FormatException( String message )
    {
        super( message );
    }

    public FormatException( String message, Throwable cause )
    {
        super( message, cause );
    }


}
