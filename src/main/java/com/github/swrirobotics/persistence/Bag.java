// *****************************************************************************
//
// Copyright (c) 2015, Southwest Research Institute® (SwRI®)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//     * Neither the name of Southwest Research Institute® (SwRI®) nor the
//       names of its contributors may be used to endorse or promote products
//       derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL Southwest Research Institute® BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.
//
// *****************************************************************************

package com.github.swrirobotics.persistence;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="bags")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@NamedNativeQuery(name = "Bag.countBagPaths",
                  query = "SELECT COUNT(b.path) AS count, c.path AS path " +
                          "FROM bags b " +
                          "RIGHT JOIN (SELECT a.path FROM bags a GROUP BY a.path) c " +
                          "  ON b.path LIKE CONCAT(c.path, '%') AND " +
                          "    LOWER(b.filename) LIKE CONCAT('%', LOWER(:text), '%') " +
                          "GROUP BY c.path",
                  resultClass = BagCount.class)
public class Bag implements Serializable {
    private Long id;

    // All of these fields are inherent properties of or are extracted from the bag
    // file; they shouldn't ever be changed by a user.
    private String filename; // The file name of the bag; "data.bag"
    private String path; // The path leading to the bag, with a trailing slash; "/bags/"
    private String version; // The version; should always be "2.0"
    private Double duration; // The duration, in seconds
    private Timestamp startTime; // The earliest time recorded
    private Timestamp endTime; // The latest time recorded
    private Long size; // Size in Megabytes
    private Long messageCount; // Total number of messages
    private Boolean indexed; // If the bag is indexed
    private Boolean compressed; // If the bag has bz2-compressed chunks
    private Set<MessageType> messageTypes = new HashSet<>(); // All message types used in the bag
    private Set<Topic> topics = new HashSet<>(); // All topics published by the bag
    private Timestamp createdOn; // When the bag was created
    private Boolean missing; // If the actual file is missing from the filesystem
    private List<BagPosition> bagPositions = new ArrayList<>(); // GPS positions extracted from the bag
    private Boolean hasPath; // If we have any positions; testing this is faster than checking bagPositions.isEmpty
    private Point coordinate;
    private String storageId; // Unique identifier for the storage mechanism used for this bag

    // The following fields are metadata about a bag file that can be modified
    // by a user.
    private String vehicle; // Name of the vehicle
    private String description; // Description of the bag
    private String md5sum; // Unique ID generated by BagFile
    private String location; // Reverse-Geocoded physical location; "100 Example St, San Antonio TX"
    private Set<Tag> tags = new HashSet<>(); // User-entered tags
    private Timestamp updatedOn; // Last time the DB entry was modified

    // The following are filled in when displaying bags in a tree view; they
    // are not stored in the database.
    private String parentId;
    private Boolean expanded = true;
    private Boolean leaf = true;

    // These fields do not actually exist in the DB; they are generated by
    // spatial functions as necessary.
    private Double latitudeDeg;
    private Double longitudeDeg;

    private Set<ScriptResult> scriptResults = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable = false, length = 100)
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Column(nullable = false, length = 255)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Column(nullable = false, length = 10)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    @Column(nullable = false)
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Column(nullable = false)
    public Long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Long messageCount) {
        this.messageCount = messageCount;
    }

    @Column(nullable = false)
    public Boolean getIndexed() {
        return indexed;
    }

    public void setIndexed(Boolean indexed) {
        this.indexed = indexed;
    }

    @Column(nullable = false)
    public Boolean getCompressed() {
        return compressed;
    }

    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }

    @ManyToMany(fetch = FetchType.EAGER,
                cascade={CascadeType.REFRESH, CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name="bag_message_types",
            joinColumns = {@JoinColumn(name="bag_id", referencedColumnName="id")},
            inverseJoinColumns = {@JoinColumn(name="message_type_name", referencedColumnName = "name"),
                    @JoinColumn(name="message_type_md5sum", referencedColumnName = "md5sum")})
    public Set<MessageType> getMessageTypes() {
        return messageTypes;
    }

    private void setMessageTypes(Set<MessageType> messageTypes) {
        this.messageTypes = messageTypes;
    }

    @OneToMany(mappedBy = "bag",
               cascade={CascadeType.REFRESH, CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.EAGER)
    public Set<Topic> getTopics() {
        return topics;
    }

    private void setTopics(Set<Topic> topics) {
        this.topics = topics;
    }

    @Column(nullable = false)
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Column(nullable = false)
    public Boolean getMissing() {
        return missing;
    }

    public void setMissing(Boolean missing) {
        this.missing = missing;
    }

    @OneToMany(mappedBy = "bag",
               cascade={CascadeType.REFRESH, CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    public List<BagPosition> getBagPositions() {
        return bagPositions;
    }

    private void setBagPositions(List<BagPosition> bagPositions) {
        this.bagPositions = bagPositions;
    }

    public Boolean getHasPath() {
        return hasPath != null && hasPath;
    }

    public void setHasPath(Boolean hasPath) {
        this.hasPath = hasPath;
    }

    @Column(length = 100)
    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(length = 32, nullable = false, unique = true)
    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    @Column(length = 100)
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Column(name = "latitudeDeg")
    @Formula("ST_Y(coordinate)")
    public Double getLatitudeDeg() {
        return latitudeDeg;
    }

    private void setLatitudeDeg(Double latitudeDeg) {
        this.latitudeDeg = latitudeDeg;
    }

    @JsonIgnore
    public Point getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Point coordinate) {
        this.coordinate = coordinate;
    }

    @Column(name = "longitudeDeg")
    @Formula("ST_X(coordinate)")
    public Double getLongitudeDeg() {
        return longitudeDeg;
    }

    private void setLongitudeDeg(Double longitudeDeg) {
        this.longitudeDeg = longitudeDeg;
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    @OneToMany(mappedBy = "bag",
               cascade = {CascadeType.REFRESH, CascadeType.MERGE},
               fetch = FetchType.EAGER)
    public Set<Tag> getTags() {
        return tags;
    }

    private void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Timestamp getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Timestamp updatedOn) {
        this.updatedOn = updatedOn;
    }

    @Transient
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Transient
    public Boolean getExpanded() {
        return expanded;
    }

    public void setExpanded(Boolean expanded) {
        this.expanded = expanded;
    }

    @Transient
    public Boolean getLeaf() {
        return leaf;
    }

    public void setLeaf(Boolean leaf) {
        this.leaf = leaf;
    }

    @ManyToMany(mappedBy = "bags")
    @JsonIgnore
    public Set<ScriptResult> getScriptResults() {
        return scriptResults;
    }

    private void setScriptResults(Set<ScriptResult> scriptResults) {
        this.scriptResults = scriptResults;
    }
}
