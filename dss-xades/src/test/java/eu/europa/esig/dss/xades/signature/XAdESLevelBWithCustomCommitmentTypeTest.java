package eu.europa.esig.dss.xades.signature;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import eu.europa.esig.dss.enumerations.CommitmentType;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.XAdESTimestampParameters;

public class XAdESLevelBWithCustomCommitmentTypeTest extends AbstractXAdESTestSignature {

	private DocumentSignatureService<XAdESSignatureParameters, XAdESTimestampParameters> service;
	private XAdESSignatureParameters signatureParameters;
	private DSSDocument documentToSign;
	
	private List<CommitmentType> commitmentTypeIndications;

	@BeforeEach
	public void init() throws Exception {
		service = new XAdESService(getCompleteCertificateVerifier());
		service.setTspSource(getGoodTsa());

		documentToSign = new FileDocument(new File("src/test/resources/sample.xml"));

		signatureParameters = new XAdESSignatureParameters();
		signatureParameters.setSigningCertificate(getSigningCert());
		signatureParameters.setCertificateChain(getCertificateChain());
		signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		signatureParameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
		
		MockCommitmentType commitmentTypeApproval = new MockCommitmentType("http://nowina.lu/approved");
		commitmentTypeApproval.setDescription("Approved");
		commitmentTypeApproval.setDocumentReferences(Arrays.asList("http://nowina.lu/approved.pdf", "https://uri.etsi.org/01903/v1.2.2/ts_101903v010202p.pdf"));
		
		MockCommitmentType commitmentTypeCreation = new MockCommitmentType("http://nowina.lu/created");
		commitmentTypeCreation.setDocumentReferences(Collections.emptyList());
		
		commitmentTypeIndications = Arrays.asList(commitmentTypeApproval, commitmentTypeCreation);

		signatureParameters.bLevel()
				.setCommitmentTypeIndications(commitmentTypeIndications);

	}

	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}

	@Override
	protected DocumentSignatureService<XAdESSignatureParameters, XAdESTimestampParameters> getService() {
		return service;
	}

	@Override
	protected XAdESSignatureParameters getSignatureParameters() {
		return signatureParameters;
	}

	@Override
	protected DSSDocument getDocumentToSign() {
		return documentToSign;
	}
	
	private class MockCommitmentType implements CommitmentType {
		
		private final String uri;
		private String description;
		private List<String> documentReferences;
		
		public MockCommitmentType(String uri) {
			this.uri = uri;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setDocumentReferences(List<String> documentReferences) {
			this.documentReferences = documentReferences;
		}
		
		@Override
		public String getOid() {
			return null;
		}

		@Override
		public String getUri() {
			return uri;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public List<String> getDocumentationReferences() {
			return documentReferences;
		}
		
	}
	
}
