package assembler;

public class Main {
    public static void main(String[] args)
    {
        // parse our arguments
        if (args.length == 0)
        {
            System.out.println("Expected arguments");
        }
        else
        {
            String filename = args[0];
            System.out.println("Filename: " + filename);

            try {
                Assembler asm = new Assembler(filename);
                boolean successful = asm.assemble();
                if (successful)
                {
                    System.out.println("Success!");
                }
                else
                {
                    System.out.println("Assembly unsuccessful");
                }
            }
            catch (Exception e)
            {
                System.out.println(e.toString());
            }
            finally
            {
                System.out.println("Done");
            }
        }
    }
}
