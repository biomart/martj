package org.ensembl.mart.explorer;

public class FormatSpec {

    private String separator = null;
    private int format = -1;

    /* enums over TABULATED and FASTA, extend as needed
     * client can setFormat with fs.TABULATED or fs.FASTA
     */
	public static final int TABULATED = 1;
    public static final int FASTA = 2;

    // default constructur
    public FormatSpec() {
    }
    
    /* constructor for a fully qualified FormatSpec
     * eg., format and separator are set
     */
     public FormatSpec(int format, String separator) {
         this.format = format;
         this.separator = separator;    
     }
     
     public void setFormat(int format) {
         this.format = format;
	 }

     public int getFormat() { return format; }

     public void setSeparator(String separator) {
         this.separator = separator;
	 }
            
     public String getSeparator() {
         return separator;
	 }
}
