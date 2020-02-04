/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.pades.validation;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.cades.validation.CAdESSignature;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.identifier.TokenIdentifier;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pdf.PAdESConstants;
import eu.europa.esig.dss.pdf.PdfDssDict;
import eu.europa.esig.dss.pdf.PdfSignatureRevision;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificatePool;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CertificateRef;
import eu.europa.esig.dss.validation.PdfRevision;
import eu.europa.esig.dss.validation.PdfSignatureDictionary;
import eu.europa.esig.dss.validation.SignatureCRLSource;
import eu.europa.esig.dss.validation.SignatureCertificateSource;
import eu.europa.esig.dss.validation.SignatureDigestReference;
import eu.europa.esig.dss.validation.SignatureIdentifier;
import eu.europa.esig.dss.validation.SignatureOCSPSource;
import eu.europa.esig.dss.validation.SignatureProductionPlace;

/**
 * Implementation of AdvancedSignature for PAdES
 */
public class PAdESSignature extends CAdESSignature {

	private static final long serialVersionUID = 3818555396958720967L;

	private static final Logger LOG = LoggerFactory.getLogger(PAdESSignature.class);

	private final DSSDocument document;

	private final PdfSignatureRevision pdfSignatureRevision;

	/**
	 * The default constructor for PAdESSignature.
	 *
	 * @param document
	 * @param pdfSignatureRevision
	 * @param certPool
	 * @throws DSSException
	 */
	protected PAdESSignature(final DSSDocument document, final PdfSignatureRevision pdfSignatureRevision, final CertificatePool certPool) throws DSSException {
		super(pdfSignatureRevision.getCades().getCmsSignedData(), certPool, pdfSignatureRevision.getCades().getDetachedContents());
		this.document = document;
		this.pdfSignatureRevision = pdfSignatureRevision;
	}

	@Override
	public SignatureForm getSignatureForm() {
		if (hasPKCS7SubFilter()) {
			return SignatureForm.PKCS7;
		}
		return SignatureForm.PAdES;
	}

	@Override
	public SignatureCertificateSource getCertificateSource() {
		if (offlineCertificateSource == null) {
			offlineCertificateSource = new PAdESCertificateSource(pdfSignatureRevision.getDssDictionary(), super.getCmsSignedData(), certPool);
		}
		return offlineCertificateSource;
	}

	@Override
	public SignatureCRLSource getCRLSource() {
		if (signatureCRLSource == null) {
			signatureCRLSource = new PAdESCRLSource(pdfSignatureRevision.getDssDictionary(), getVRIKey(), getSignerInformation().getSignedAttributes());
		}
		return signatureCRLSource;
	}

	@Override
	public SignatureOCSPSource getOCSPSource() {
		if (signatureOCSPSource == null) {
			signatureOCSPSource = new PAdESOCSPSource(pdfSignatureRevision.getDssDictionary(), getVRIKey(), getSignerInformation().getSignedAttributes());
		}
		return signatureOCSPSource;
	}
	
	@Override
	public PAdESTimestampSource getTimestampSource() {
		if (signatureTimestampSource == null) {
			signatureTimestampSource = new PAdESTimestampSource(this, certPool);
		}
		return (PAdESTimestampSource) signatureTimestampSource;
	}

	@Override
	public Date getSigningTime() {
		return pdfSignatureRevision.getSigningDate();
	}

	@Override
	public SignatureProductionPlace getSignatureProductionPlace() {
		String location = pdfSignatureRevision.getPdfSigDictInfo().getLocation();
		if (Utils.isStringBlank(location)) {
			return super.getSignatureProductionPlace();
		} else {
			SignatureProductionPlace signatureProductionPlace = new SignatureProductionPlace();
			signatureProductionPlace.setCountryName(location);
			return signatureProductionPlace;
		}
	}

	@Override
	public String getContentIdentifier() {
		return null;
	}

	@Override
	public String getContentHints() {
		return null;
	}

	@Override
	public List<AdvancedSignature> getCounterSignatures() {
		/* Not applicable for PAdES */
		return Collections.emptyList();
	}

	@Override
	public List<CertificateRef> getCertificateRefs() {
		List<CertificateRef> refs = new ArrayList<>();
		// other are unsigned and should be added in the DSS Dictionary
		List<CertificateToken> encapsulatedCertificates = getCAdESSignature().getCertificateSource().getCMSSignedDataCertificates();
		addCertRefs(refs, encapsulatedCertificates);
		PdfDssDict dssDictionary = pdfSignatureRevision.getDssDictionary();
		if (dssDictionary != null) {
			Map<Long, CertificateToken> certMap = dssDictionary.getCERTs();
			addCertRefs(refs, certMap.values());
		}
		return refs;
	}

	private void addCertRefs(List<CertificateRef> refs, Collection<CertificateToken> encapsulatedCertificates) {
		for (CertificateToken certificateToken : encapsulatedCertificates) {
			CertificateRef ref = new CertificateRef();
			ref.setCertDigest(new Digest(DigestAlgorithm.SHA1, certificateToken.getDigest(DigestAlgorithm.SHA1)));
			refs.add(ref);
		}
	}

	/**
	 * @return the CAdES signature underlying this PAdES signature
	 */
	public CAdESSignature getCAdESSignature() {
		return pdfSignatureRevision.getCades();
	}
	
	@Override
	public SignatureIdentifier buildSignatureIdentifier() {
		final CertificateToken certificateToken = getSigningCertificateToken();
		final TokenIdentifier identifier = certificateToken == null ? null : certificateToken.getDSSId();
		return SignatureIdentifier.buildSignatureIdentifier(getSigningTime(), identifier, getDigestOfByteRange());
	}

	private String getDigestOfByteRange() {
		int[] signatureByteRange = pdfSignatureRevision.getSignatureByteRange();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i : signatureByteRange) {
			baos.write(i);
		}
		return DSSUtils.getMD5Digest(baos.toByteArray());
	}

	public int[] getSignatureByteRange() {
		return pdfSignatureRevision.getSignatureByteRange();
	}
	
	/**
	 * TS 119 442 - V1.1.1 - Electronic Signatures and Infrastructures (ESI), ch. 5.1.4.2.1.3 XML component:
	 * 
	 * In case of PAdES signatures, the input of the digest value computation shall be the result of decoding the
	 * hexadecimal string present within the Contents field of the Signature PDF dictionary enclosing one PAdES
	 * digital signature. 
	 */
	@Override
	public SignatureDigestReference getSignatureDigestReference(DigestAlgorithm digestAlgorithm) {
		byte[] contents = pdfSignatureRevision.getContents();
		byte[] digestValue = DSSUtils.digest(digestAlgorithm, contents);
		return new SignatureDigestReference(new Digest(digestAlgorithm, digestValue));
	}

	@Override
	public boolean isDataForSignatureLevelPresent(SignatureLevel signatureLevel) {
		boolean dataForLevelPresent = true;
		switch (signatureLevel) {
		case PDF_NOT_ETSI:
			break;
		case PAdES_BASELINE_LTA:
			dataForLevelPresent = hasLTAProfile() && hasLTProfile() && hasCAdESDetachedSubFilter();
			break;
		case PKCS7_LTA:
			dataForLevelPresent = hasLTAProfile() && hasLTProfile() && hasPKCS7SubFilter();
			break;
		case PAdES_BASELINE_LT:
			dataForLevelPresent = hasLTProfile() && hasDSSDictionary() && (hasTProfile() || hasLTAProfile()) && hasCAdESDetachedSubFilter();
			break;
		case PKCS7_LT:
			dataForLevelPresent = hasLTProfile() && (hasTProfile() || hasLTAProfile()) && hasPKCS7SubFilter();
			break;
		case PAdES_BASELINE_T:
			dataForLevelPresent = hasTProfile() && hasCAdESDetachedSubFilter();
			break;
		case PKCS7_T:
			dataForLevelPresent = hasTProfile() && hasPKCS7SubFilter();
			break;
		case PAdES_BASELINE_B:
			dataForLevelPresent = hasCAdESDetachedSubFilter();
			break;
		case PKCS7_B:
			dataForLevelPresent = hasPKCS7SubFilter();
			break;
		default:
			throw new IllegalArgumentException("Unknown level " + signatureLevel);
		}
		LOG.debug("Level {} found on document {} = {}", signatureLevel, document.getName(), dataForLevelPresent);
		return dataForLevelPresent;
	}
	
	private boolean hasDSSDictionary() {
		return getDssDictionary() != null;
	}
	
	public PdfDssDict getDssDictionary() {
		return pdfSignatureRevision.getDssDictionary();
	}

	private boolean hasCAdESDetachedSubFilter() {
		return (pdfSignatureRevision != null) && PAdESConstants.SIGNATURE_DEFAULT_SUBFILTER.equals(pdfSignatureRevision.getPdfSigDictInfo().getSubFilter());
	}

	private boolean hasPKCS7SubFilter() {
		return (pdfSignatureRevision != null) && PAdESConstants.SIGNATURE_PKCS7_SUBFILTER.equals(pdfSignatureRevision.getPdfSigDictInfo().getSubFilter());
	}

	@Override
	public SignatureLevel[] getSignatureLevels() {
		return new SignatureLevel[] { SignatureLevel.PDF_NOT_ETSI, SignatureLevel.PAdES_BASELINE_B, SignatureLevel.PKCS7_B, SignatureLevel.PAdES_BASELINE_T,
				SignatureLevel.PKCS7_T, SignatureLevel.PAdES_BASELINE_LT, SignatureLevel.PKCS7_LT, SignatureLevel.PAdES_BASELINE_LTA,
				SignatureLevel.PKCS7_LTA };
	}

	public boolean hasOuterSignatures() {
		return Utils.isCollectionNotEmpty(pdfSignatureRevision.getOuterSignatures());
	}

	@Override
	public PdfRevision getPdfRevision() {
		return pdfSignatureRevision;
	}
	
	public PdfSignatureDictionary getPdfSignatureDictionary() {
		return pdfSignatureRevision.getPdfSigDictInfo();
	}
	
	/**
	 * Name of the related to the signature VRI dictionary
	 * @return related {@link String} VRI dictionary name
	 */
	public String getVRIKey() {
		// By ETSI EN 319 142-1 V1.1.1, VRI dictionary's name is the base-16-encoded (uppercase)
		// SHA1 digest of the signature to which it applies
		return pdfSignatureRevision.uniqueId().toUpperCase();
	}

}
